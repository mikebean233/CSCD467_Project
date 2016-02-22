public class ThreadManager extends Thread{
    private ThreadPool _threadPool;
    private JobQueue _jobQueue;
    private int _lowThreshold, _highThreshold, _pollPeriod, _minThreadCount, _maxThreadCount;

    public ThreadManager(ThreadPool threadPool, JobQueue jobQueue, int lowThreshold, int highThreshold, int pollPeriod){
        super("Thread Manager");
        if(    threadPool == null
            || jobQueue   == null
            || lowThreshold  > highThreshold
            || lowThreshold  < 0
            || highThreshold > jobQueue.maxSize() - 1
            || pollPeriod    < 0)
            throw new IllegalArgumentException();

        _threadPool = threadPool;
        _jobQueue = jobQueue;
        _lowThreshold = lowThreshold;
        _highThreshold = highThreshold;
        _minThreadCount = _threadPool.minThreadCount();
        _maxThreadCount = _threadPool.maxCapacity();
        _pollPeriod = pollPeriod;
        this.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                if(_threadPool.stopped())
                    throw new InterruptedException();
                int currentJobCount = _jobQueue.length();
                int currentThreadCount = _threadPool.numberThreads();
                int noWaitingThreads = _threadPool.getWaitingCount();

                if (    // we have reached the lower threshold
                        currentJobCount         <  _lowThreshold
                        // if we half the number of current threads, we will have at least the min left
                        && ((currentThreadCount / 2) >= _minThreadCount)
                        // make sure there are enough waiting threads to get rid of
                        && (noWaitingThreads)        >= (currentThreadCount - (currentThreadCount / 2) )) {

                    _threadPool.decreaseThreadsInPool();
                }
                else if (currentJobCount >= _highThreshold && (currentThreadCount * 2) <= (_maxThreadCount - currentThreadCount)) {
                    _threadPool.increaseThreadsInPool();
                }
                if(this.isInterrupted())
                    throw new InterruptedException();
                this.sleep(_pollPeriod);
                }
            }
            catch(InterruptedException iE) {
            }
    }// end run
}// end ThreadManger

