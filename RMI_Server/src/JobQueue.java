import java.net.Socket;

public class JobQueue {
    private SharedQueue<Socket> _jobQueue;
    private int _jobQueueSize;
    private Object _addJobLock, _fetchJobLock;

    public JobQueue(int jobQueueSize){

        if(jobQueueSize < 1)
            throw new IllegalArgumentException("Job Queue size must be at least one: " + jobQueueSize);
        _jobQueueSize = jobQueueSize;
        _addJobLock = new Object();
        _fetchJobLock = new Object();
        _jobQueue = new SharedQueue<>();
    }

    public boolean addJob(Socket thisJob){
        if(thisJob == null)
            throw new IllegalArgumentException();
        synchronized (_addJobLock){
            // If there is no more room in the job queue, let the caller know (return false);
            if(_jobQueue.length() == _jobQueueSize)
                return false;

            _jobQueue.enqueue(thisJob);
            synchronized (_fetchJobLock) {
                _fetchJobLock.notifyAll();
            }
        }
        return true;

    }

    public Socket fetchJob() throws InterruptedException{
        synchronized (_fetchJobLock) {

            while(!_jobQueue.any())
                _fetchJobLock.wait();

            return _jobQueue.dequeue();
        }
    }


    public int maxSize(){return _jobQueueSize;}
    public boolean Any(){return _jobQueue.any();}
    public int length(){ return _jobQueue.length();}
}
