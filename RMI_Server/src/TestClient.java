import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args){
        TestClient thisTestClient = new TestClient(100);
        thisTestClient.start();
    }


    private int _noThreads;

    public TestClient(int noThreads){
        if(noThreads < 1)
            throw new IllegalArgumentException();
        _noThreads = noThreads;
    }

    public void start(){
        int clientThreadCount = 0;
        while(clientThreadCount < _noThreads){
            try{

                Socket newSocket = new Socket("localhost", 9898);
                ClientThread thisThread = new ClientThread(newSocket, "Client Thread " + (clientThreadCount++));
                thisThread.start();

            }
            catch(Exception ex){
                break;
            }
        }

    }

    private class ClientThread extends Thread {
        private Socket thisSocket;
        private PrintWriter printWriter;
        public ClientThread(Socket socket, String name) throws Exception{
            super(name);
            thisSocket = socket;
            printWriter = new PrintWriter(socket.getOutputStream(), true);

        }

        @Override
        public void run(){
            try {
                printWriter.println(this.getName() + " says hi");
                printWriter.flush();
                this.sleep(1000);
                printWriter.close();
                thisSocket.close();
            }
            catch(InterruptedException e){
                System.out.println(this.getName() + " received interrupt");
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

    }
}
