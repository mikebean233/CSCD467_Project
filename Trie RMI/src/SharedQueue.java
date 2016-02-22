public class SharedQueue<E> {
    private int _capacity, _size;
    private SharedQueueNode<E> _front, _rear;
    private Object _frontLock, _rearLock, _sizeLock; // locks
    private int _matchCount, _lineCount;
    private boolean _finished;
    private static final int DEFAULT_MAX_SIZE = 100;

    public SharedQueue(int capacity) throws IllegalArgumentException{
        if(capacity < 1)
            throw new IllegalArgumentException("Invalid size of SharedQueue: " + capacity);
        _size = 0;
        _capacity = capacity;
        _frontLock = new Object();
        _rearLock = new Object();
        _sizeLock = new Object();
        _matchCount = 0;
        _lineCount = 0;
        _finished = false;
    }

    public SharedQueue(){
        this(DEFAULT_MAX_SIZE);
    }

    public void setFinished(){
        _finished = true;
        synchronized (_rearLock){
            _rearLock.notifyAll();
        }
    }

    public void setMatchCount(int count){_matchCount = count;}

    public int getMatchCount(){return _matchCount;}

    public int getLineCount(){return _lineCount;}

    public void setLineCount(int count){_lineCount = count;}

    public int capacity(){return _capacity;}

    public synchronized boolean any(){
        return _size > 0;
    }

    public synchronized boolean full(){
        return length() == _capacity;
    }

    public SharedQueue enqueue(E newValue){
        if(newValue == null)
            throw new NullPointerException();

        synchronized (_frontLock) {
            //if(length() == _capacity)
            //    throw new IllegalStateException("SharedQueue full");
            while(length() == _capacity){
                try {
                    _frontLock.wait(); // wait for things to be taken out of the queue
                }
                catch(InterruptedException e){
                    return this;
                }
            }

            if (_front == null) {
                synchronized (_rearLock) {
                    _front = _rear = new SharedQueueNode<>(null, newValue, null);
                    _rearLock.notifyAll();
                }
            } else {
                _front.setNext(new SharedQueueNode<E>(_front, newValue, null));
                _front = _front.getNext();
            }
            incrementSize();
            return this;
        }
    }

    public E dequeue() throws InterruptedException{
        synchronized (_rearLock) {
            while(_rear == null){
                    if(_finished)
                        return null;
                    _rearLock.wait(); // wait for there to be something in the queue
            }

            E rearValue = _rear.value();

            synchronized (_frontLock) {
                if (_size == 1){
                    _front = _rear = null;
                    _size = 0;
                    return rearValue;
                }
            }
            _rear = _rear.getNext();
            _rear.setPrev(null);
            decrementSize();
            if(_size == _capacity - 1)
                synchronized (_frontLock) {
                    _frontLock.notifyAll();// If there might be threads waiting for there to be room in the queue, wake them up
                }
            return rearValue;
        }
    }

    private void incrementSize(){
        synchronized (_sizeLock){
            ++_size;
        }
    }

    private void decrementSize(){
        synchronized (_sizeLock){
            --_size;
        }
    }

    public int length(){
        synchronized (_sizeLock){
            return _size;
        }
    }

    private class SharedQueueNode <T> {
        private T _value;
        public SharedQueueNode <T> _prev, _next;

        public SharedQueueNode(SharedQueueNode <T> prev, T value, SharedQueueNode <T> next) {
            _value = value;
            _prev = prev;
            _next = next;
        }

        @Override
        public boolean equals(Object that){
            if(that == null)
                return false;
            return _value.equals(that);
        }

        public T value(){
            return _value;
        }

        public SharedQueueNode<T> getPrev(){return _prev;}

        public SharedQueueNode<T> getNext(){
            return _next;
        }

        public void setPrev(SharedQueueNode<T> prev){
            _prev = prev;
        }

        public void setNext(SharedQueueNode<T> next){
            _next = next;
        }
    }
}
