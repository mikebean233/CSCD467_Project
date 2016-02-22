import java.io.*;
import java.net.Socket;

public class WorkerThread extends Thread {
    private JobQueue _jobQueue;
    private ThreadPool _threadPool;
    private static int _noInstances = 0;
    private int _threadId;

    WorkerThread(JobQueue jobQueue, ThreadPool threadPool) {
        super("Worker Thread " + _noInstances);
        if (jobQueue == null || threadPool == null)
            throw new IllegalArgumentException();
        _jobQueue = jobQueue;
        _threadPool = threadPool;
        _threadId = _noInstances++;
    }


    @Override
    public boolean equals(Object that){
        if(!(that instanceof WorkerThread))
            return false;

        WorkerThread thatWorker = (WorkerThread) that;
        return _threadId == thatWorker._threadId;
    }


    @Override
    public void run() {
        while (true) {
            try {
                _threadPool.moveThreadToWaitingList(this);
                Socket thisSocket = _jobQueue.fetchJob();
                _threadPool.moveThreadToRunningList(this);

                processJob(thisSocket);
                if(_threadPool.stopped())
                    throw new InterruptedException();
            } catch (InterruptedException Ie) {
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
    }

    private void processJob(Socket clientSocket) throws Exception {
        String output = "";
        String command = "";
        String[] tokens;
        InterruptedException interrupt = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        while (!clientSocket.isClosed()) {
            try {
                output = "";
                try {
                    command = in.readLine().toLowerCase();
                    if (isInterrupted())
                        throw new InterruptedException();
                }
                catch(NullPointerException Npe){
                    throw new InterruptedIOException();
                }

                // The end of the stream has been reached (the connection was lost)
                if(command == null)
                    break;
                if(command.equals("kill")){
                    output = "Killing server thread ...";
                    _threadPool.stopPool();
                    break;
                }

                tokens = command.split(",");


                if (tokens.length < 3 || tokens.length > 3)
                    output = "Error processing command: " + command;

                if (tokens.length == 3) {
                    // we actually have a command
                    int firstParameter = Integer.parseInt(tokens[1]);
                    int secondParameter = Integer.parseInt(tokens[2]);

                    switch (tokens[0]) {
                        case "add":
                            output = "" + (firstParameter + secondParameter);
                            break;
                        case "sub":
                            output = "" + (firstParameter - secondParameter);
                            break;
                        case "mul":
                            output = "" + (firstParameter * secondParameter);
                            break;
                        case "div":
                            if (secondParameter == 0)
                                output = "Error processing command: " + command + " cannot divide by zero";
                            else
                                output = "" + (firstParameter / secondParameter);
                            break;
                        default:
                            output = "Unknown command: " + command;
                    }
                }

                if (isInterrupted())
                    throw new InterruptedException();
            } catch (NumberFormatException Fe) {
                output = "Error parsing number in command \"" + command + "\": " + Fe.getMessage();
            } catch (InterruptedIOException Ie) {
                //output += "\nServer thread exiting...";
                //interrupt = new InterruptedException();
                break;
            } catch (InterruptedException intEx) {
                interrupt = intEx;
                break;
            } catch (IOException IoE) {
                break;
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            try {
                out.write(output + "\n");
                out.flush();
            } catch (Exception e) {
            }


        }
        try{
            clientSocket.close();
            in.close();
            out.close();
        }catch(Exception e){}

        if (interrupt != null)
            throw interrupt;

    }
}