import java.net.ServerSocket;
import java.util.ArrayList;

public class ThreadPool
{
    private int _maxCapacity;
    private int _minThreadCount;
    private int _actualNumberThreads;
    private ArrayList<WorkerThread> _runningThreads;
    private ArrayList<WorkerThread> _waitingThreads;
    private boolean _stopped;
    private JobQueue _jobQueue;
    private Object _threadCountLock;
    private ServerSocket _mainThreadSocket;
    private Trie _trie;

    public ThreadPool(int startingThreadCount, int maxCapacity, JobQueue jobQueue, ServerSocket mainThreadSocket, Trie trie){
        if(jobQueue == null
                || mainThreadSocket == null
                || startingThreadCount < 1
                || startingThreadCount > maxCapacity
                || trie == null)
            throw new IllegalArgumentException();

        _maxCapacity = maxCapacity;
        _actualNumberThreads = startingThreadCount;
        _minThreadCount = startingThreadCount;
        _runningThreads = new ArrayList<>();
        _waitingThreads = new ArrayList<>();
        _jobQueue = jobQueue;
        _threadCountLock = new Object();
        _mainThreadSocket = mainThreadSocket;
        _trie = trie;
        startPool(_minThreadCount = startingThreadCount);
    }

    public Trie getTrie(){return _trie;}

    private void startPool(int initialThreadCount) {
        for(int i = 0; i < initialThreadCount; ++i){
            WorkerThread thisWorkerThread = new WorkerThread(_jobQueue, this);
            _waitingThreads.add(thisWorkerThread);
            thisWorkerThread.start();
        }

        int waitingCount = 0;
        while(waitingCount < initialThreadCount){
            waitingCount = 0;
            for(int i = 0; i < initialThreadCount; ++i)
                waitingCount += (_waitingThreads.get(i).getState() == Thread.State.WAITING) ? 1 : 0;
        }
        _actualNumberThreads = initialThreadCount;

    }

    public int getWaitingCount(){
        synchronized (_waitingThreads) {
            return _waitingThreads.size();
        }
    }

    public void moveThreadToWaitingList(WorkerThread workerThread){
        if(workerThread == null){
            throw new IllegalArgumentException();
        }
        synchronized (_runningThreads) {
            if (_runningThreads.contains(workerThread))
                _runningThreads.remove(workerThread);
            else
                return;
        }

        synchronized (_waitingThreads){
            _waitingThreads.add(workerThread);
        }
    }

    public void moveThreadToRunningList(WorkerThread workerThread){
        if(workerThread == null){
            throw new IllegalArgumentException();
        }

        synchronized (_waitingThreads){
            if(_waitingThreads.contains(workerThread))
                _waitingThreads.remove(workerThread);
            else
                return;
        }

        synchronized (_runningThreads) {
            _runningThreads.add(workerThread);
        }
    }


    public void increaseThreadsInPool() {
        synchronized (_threadCountLock) {
            if(_stopped)
                return;

            int newThreadCount = Math.min(_actualNumberThreads * 2, _maxCapacity);
            int noThreadsToCreate = newThreadCount - _actualNumberThreads;
            if(noThreadsToCreate == 0){
                return;
            }

            synchronized (_waitingThreads){
                for(int i = 0; i < noThreadsToCreate; ++i) {
                    WorkerThread thisThread = new WorkerThread(_jobQueue, this);
                    _waitingThreads.add(thisThread);
                    thisThread.start();
                }
            }

            _actualNumberThreads = newThreadCount;

        }
    }

    public void decreaseThreadsInPool() {
        //halve the threads in pool according to threshold ...
        if(_stopped)
            return;
        synchronized (_threadCountLock) {
            synchronized (_waitingThreads) {
                int newThreadCount = Math.max(_actualNumberThreads / 2, _minThreadCount);
                int noThreadsToRemove = _actualNumberThreads - newThreadCount;
                if (noThreadsToRemove == 0 || _waitingThreads.isEmpty()) {
                    return;
                }


                if (_waitingThreads.size() < noThreadsToRemove) {
                    return;
                }

                for (int i = 0; i < noThreadsToRemove; ++i) {
                    WorkerThread thisThread = _waitingThreads.remove(0);
                    thisThread.interrupt();
                }
                _actualNumberThreads = newThreadCount;
            }
        }
    }

    public boolean stopped(){return _stopped;}

    public void stopPool(){
        (new Thread(new Runnable(){
        @Override
        public void run(){
            synchronized (_runningThreads) {
                for (WorkerThread thisThread : _runningThreads) {
                    thisThread.interrupt();
                    try{thisThread.join();}catch(Exception ex){}
                }
                _runningThreads.clear();
            }

            synchronized (_waitingThreads) {
                for (WorkerThread thisThread : _waitingThreads) {
                    thisThread.interrupt();
                    try{thisThread.join();}catch(Exception ex){}
                }
                _waitingThreads.clear();
            }
            try{_mainThreadSocket.close();}catch(Exception ex){};
        }
        })).start();
    }

    public int numberThreads() {
        return _actualNumberThreads;
    }

    public int maxCapacity() {
        return _maxCapacity;
    }

    public int minThreadCount(){return _minThreadCount;}
    //and other methods as you need.
}