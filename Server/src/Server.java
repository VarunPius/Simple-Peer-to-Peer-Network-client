import java.io.*;
        import java.net.*;

/**
 * ----------------------------------------------
 * Author   : Varun Pius Felix Rodrigues       --
 * UFID     : 61965993                         --
 * GatorLink: varunpius@ufl.edu                --
 * Computer Networks Project                   --
 * Server class java file                      --
 * Created by VarunPius.                       --
 *                                             --
 * ----------------------------------------------
 * Edit History:                               --
 * ----------------------------------------------
 * Create/ChangeLog:                Date:      --
 * -------------------------------  -------------
 * Split method                     2015-11-11 --
 * Socket method                    2015-11-12 --
 * Socket file sending single file  2015-11-14 --
 * Piece creation at client         2015-11-19 --
 * Created separate client2         2015-11-20
 * Completed chunk send logic       2015-11-20 --
 *      -Issue: last chunk                     --
 * Piece merging at Client          2015-11-20 --
 * Threading                        2015-11-22 --
 *  --
 * ----------------------------------------------
 */

public class Server {
    public static int segSize = 100 * 1024;

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter File name: ");
        String file_name = br.readLine();

        Server s = new Server();
        int segNos = s.Split(file_name);

        s.socket_start(file_name, segNos);
    }

    public int Split(String file_name) {
        int total_Seg = 0;
        try {
            String fN = file_name;

            File file = new File(fN);
            long fileSize = file.length();

            System.out.println("File size is: " + fileSize);

            String new_fileName;
            InputStream ipFileStrm;
            OutputStream opFileStrm;
            byte[] partFileArray;
            int rdLen = segSize;
            int rd;

            ipFileStrm = new FileInputStream(fN);
            while (fileSize > 0) {
                if (fileSize <= segSize) {
                    rdLen = (int) fileSize;
                }

                partFileArray = new byte[rdLen];
                rd = ipFileStrm.read(partFileArray, 0, rdLen);
                fileSize -= rd;
                total_Seg++;
                new_fileName = fN + ".part" + Integer.toString(total_Seg);

                opFileStrm = new FileOutputStream(new File(new_fileName));
                opFileStrm.write(partFileArray);
                opFileStrm.flush();
                opFileStrm.close();
            }
        } catch (IOException err) {
            err.printStackTrace();
        }
        return total_Seg;
    }

    public void socket_start(String file_name, int segNos) {
        try {
            String fN = file_name;
            int sN = segNos;
            int clNum = 0;

            System.out.println("Total nos of segments is: " + sN);

            int socketNos = getServerSocket();
            System.out.println("Server Starting; configured to listen on port: " + socketNos);

            ServerSocket servSocket = new ServerSocket(socketNos);
            while (true) {
                clNum++;
                System.out.println("Waiting for connections");
                Socket clSocket = servSocket.accept();
                System.out.println("Server connected to " + clSocket.getInetAddress().getHostName());

                socket_client SC = new socket_client(clSocket, fN, sN, clNum);
                SC.start();
                if (clNum == 5)
                    clNum = 0;

            }
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    public int getServerSocket()
    {
        String line;
        int ScktNos = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("config.txt"));

            while ((line = br.readLine()) != null) {
                int tempCl = Character.getNumericValue(line.charAt(0));
                if (tempCl == 1) {
                    ScktNos = Integer.parseInt(line.substring(2, 6));
                    System.out.println("Server Socket is: " + ScktNos);
                }
            }
            br.close();
        }
        catch(IOException er)
        {
            er.printStackTrace();
        }
        return ScktNos;
    }
}

class socket_client extends Thread
{
    private Socket clSocket;
    private String file_name;
    private int segNos;
    private int clNum;

    DataOutputStream os;

    public socket_client(Socket clSocket, String file_name, int segNos, int clNum)
    {
        this.clSocket = clSocket;
        this.file_name = file_name;
        this.segNos = segNos;
        this.clNum = clNum;
    }

    public void run()
    {
        System.out.println("Loop begins!");
        try
        {
            PrintWriter out = new PrintWriter(clSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clSocket.getInputStream()));

            System.out.println("Sending name here");

            //Sending name of file and number of segments
            out.println(file_name);

            out.println(segNos);
            System.out.println("total number of segments: " + segNos);

            out.println(clNum);
            System.out.println("Client nos: " + clNum );
            os = new DataOutputStream(clSocket.getOutputStream());

            for (int i = clNum; (i <= segNos); i += 5)
            {
                System.out.println("Inside Loop: " + i);
                String segName = file_name + ".part" + i;

                File file = new File(segName);
                long segSize = file.length();
                System.out.println("Segment size is: " + segSize);

                byte[] bufferByteArray = new byte[(int) segSize];

                InputStream is = new FileInputStream(file);

                int c = is.read(bufferByteArray, 0, (int) segSize);
                System.out.println("read buffer len " + c);

                System.out.println("Segment: " + segName + " -> sending in progress");

                os.write(bufferByteArray, 0, bufferByteArray.length);
                os.flush();

                bufferByteArray = null;

                System.out.println("File Sent!");
                //is.close();
                //os.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}




