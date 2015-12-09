import com.sun.org.apache.xpath.internal.SourceTree;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ----------------------------------------------
 * Author   : Varun Pius Felix Rodrigues       --
 * UFID     : 61965993                         --
 * GatorLink: varunpius@ufl.edu                --
 * Computer Networks Project                   --
 * Client1 class java file                     --
 * Created by VarunPius.                       --
 *                                             --
 * ----------------------------------------------
 * Edit History:                               --
 * ----------------------------------------------
 * Create/ChangeLog:                Date:      --
 * -------------------------------  -------------
 * Socket method                    2015-11-12 --
 * Single file Transfer             2015-11-14 --
 * Pieces creation                  2015-11-19 --
 * Separate method for socket       2015-11-19 --
 * **Started new class Client2**    2015-11-20 --
 * Chunk Receive                    2015-11-20 --
 * File merge                       2015-11-22 --
 * Multiple client creation         2015-11-23 --
 * Creating separate threads        2015-11-24 --
 *      for download                2015-11-24 --
 * Sending files to immediate                  --
 *      peer                        2015-11-29 --
 * Sending files to all peers       2015-11-30 --
 * Update merge logic               2015-11-30 --
 * ----------------------------------------------
 */

public class Client5
{
    public static int segSize = 100*1024;

    public static void main(String[] args) throws IOException
    {
        Client5 cl1 = new Client5();
        cl1.socket_clnt();
    }

    public void socket_clnt() throws IOException
    {
        System.out.println("Client 1 starting; initiating connection");
        int rd = 0;

        int serverSocketNos = getServerSocket();
        System.out.println("Server Starting; configured to listen on port: " + serverSocketNos);

        Socket rqSocket = new Socket("localhost", serverSocketNos);        //Creating request socket to request file from Server

        PrintWriter out = new PrintWriter(rqSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(rqSocket.getInputStream()));

        //File Details:
        String file_name = in.readLine();
        System.out.println("Name of the file is: " + file_name);

        int totalSeg = Integer.parseInt(in.readLine());
        System.out.println("Total number of segments:" + totalSeg);

        int cl_num = Integer.parseInt(in.readLine());

        InputStream is = rqSocket.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte buf[]=new byte[1000000000];
        int len;
        int countLastSeq = 0;
        while(((len=is.read(buf)) != -1))
        {
            System.out.println("While " + len);
            baos.write(buf, 0, len);
            countLastSeq = countLastSeq + len;
            System.out.println("Read buffer len: " + len);
        }

        System.out.println("Count Last Seq: " + countLastSeq);
        System.out.println("read buffer len: " + len);

        PrintWriter writerx = new PrintWriter("ChunkList.txt");
        writerx.print("");
        writerx.close();

        for (int i = cl_num; (i <= totalSeg); i += 5)
        {
            System.out.println("Inside loop: " + i);
            System.out.println("Segment number: " + i);

            String fileStore = file_name + ".part" + i; //"D:\\CodeTrials\\CN_P2P\\Client2\\" + file_name + ".part" + i;
            System.out.println("File location: D:\\CodeTrials\\CN_P2P\\Client2\\" + fileStore);
            OutputStream os = new FileOutputStream(fileStore);

            if (countLastSeq >= 102400) {
                os.write(buf, rd, 102400);
                os.flush();
                rd = rd + 102400;
                BufferedWriter bw = new BufferedWriter(new FileWriter("ChunkList.txt", true));
                bw.write("" + i);
                bw.newLine();
                bw.flush();
                bw.close();
                os.close();
            }
            else if (countLastSeq < 102400)
            {
                os.write(buf, rd, countLastSeq);
                rd = rd + 102400;

                BufferedWriter bw = new BufferedWriter(new FileWriter("ChunkList.txt", true));
                bw.write("" + i);
                bw.newLine();
                bw.flush();
                bw.close();
                os.close();
                break;
            }

            countLastSeq = countLastSeq - 102400;
        }

        //Threads creation and starting them;
        ClientClient CC = new ClientClient(file_name, cl_num, totalSeg);
        CC.start();
        ClientServer CS = new ClientServer(file_name, cl_num, totalSeg);
        CS.start();
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

class ClientClient extends Thread
{
    private Socket clSocket;
    private String file_name;
    private int segNos;
    private int clNum;
    private int totalSeg;

    String line;
    int dwldCl;
    int upldCl;
    int dwldScktNos;
    int rd = 0;

    Socket dwldSckt;
    ObjectOutputStream out;
    ObjectInputStream in;
    String chunkListStr = "";
    List<String> chunkListStrList = new ArrayList<String>();
    List<Integer> chunkList = new ArrayList<Integer>();
    List<Integer> ClchunkListArray = new ArrayList<Integer>();
    String chunkRqd;

    PrintWriter pw;
    BufferedReader br;

    public ClientClient(String file_name, int clNum, int totalSeg)
    {
        this.file_name = file_name;

        this.clNum = clNum;
        this.totalSeg = totalSeg;
    }

    public void run()
    {
        while(true) {

            try {
                dwldCl = getDwldCl();
                dwldScktNos = getDwldScktNos();

                boolean status_chk = true;
                while (status_chk) {
                    try {
                        dwldSckt = new Socket("localhost", dwldScktNos);
                        status_chk = false;
                    } catch (ConnectException err) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ers) {
                            ers.printStackTrace();
                        }
                    }
                }

                pw = new PrintWriter(dwldSckt.getOutputStream(), true);
                br = new BufferedReader(new InputStreamReader(dwldSckt.getInputStream()));
                out = new ObjectOutputStream(dwldSckt.getOutputStream());
                in = new ObjectInputStream(dwldSckt.getInputStream());

                ClchunkListArray = getLocalChunk();

                while (true)
                {
                    int x;
                    for (x = 1; x <= totalSeg; x++) {     // : chunkList) {
                        if (ClchunkListArray.contains(x))
                            continue;
                        else {
                            chunkList = getServChunk();
                            if (chunkList.contains(x)) {
                                chunkRqd = "List: " + file_name + ".part" + x;

                                try {
                                    System.out.println("Preparing to send chunk file name.");
                                    System.out.println(chunkRqd.substring(6));

                                    String file_part = chunkRqd.substring(6);

                                    pw.println(chunkRqd);
                                    pw.flush();

                                    ObjectInputStream is = new ObjectInputStream(dwldSckt.getInputStream());
                                    //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    OutputStream os = new FileOutputStream(file_part);

                                    byte buf[] = null;

                                    try {
                                        buf = (byte[]) is.readObject();
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }

                                    os.write(buf);
                                    //rd = rd + 102400;
                                    BufferedWriter bw = new BufferedWriter(new FileWriter("ChunkList.txt", true));
                                    System.out.println("x" + x);
                                    bw.write("" + x);
                                    bw.newLine();
                                    bw.flush();
                                    bw.close();
                                    os.close();

                                    ClchunkListArray.add(x);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }

                            }
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    x = 1;

                    System.out.println("Length of Client list array: " + ClchunkListArray.size());

                    if (ClchunkListArray.size() == totalSeg)
                        break;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                fileMerge(file_name, totalSeg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getDwldCl()
    {
        try {
            BufferedReader br = new BufferedReader(new FileReader("config.txt"));

            while ((line = br.readLine()) != null) {
                int temp = Character.getNumericValue(line.charAt(0));
                if (temp == (clNum + 1)) {
                    int dwldCltmp = Character.getNumericValue(line.charAt(7));
                    dwldCl = dwldCltmp - 1;
                    System.out.println("Download Neighbor: " + dwldCl);
                }
            }
            br.close();
        }
        catch (IOException err)
        {
            err.printStackTrace();
        }

        return dwldCl;
    }

    public int getDwldScktNos()
    {
        try {
            br = new BufferedReader(new FileReader("config.txt"));

            while ((line = br.readLine()) != null) {
                int tempCl = Character.getNumericValue(line.charAt(0));
                if (tempCl == (dwldCl+1)) {
                    dwldScktNos = Integer.parseInt(line.substring(2, 6));
                    System.out.println("Download Socket is: " + dwldScktNos);
                }
            }
            br.close();
        }
        catch(IOException er)
        {
            er.printStackTrace();
        }

        return dwldScktNos;
    }

    public List<Integer> getLocalChunk()
    {
        try {
            BufferedReader br3 = new BufferedReader(new FileReader("ChunkList.txt"));

            while ((line = br3.readLine()) != null) {
                System.out.println("Reading List for ClientClient.  " + line);
                ClchunkListArray.add(Integer.parseInt(line));
                System.out.println("List of Client: " + ClchunkListArray);
            }
        }
        catch (IOException er)
        {
            er.printStackTrace();
        }
        return ClchunkListArray;
    }

    public List<Integer> getServChunk()
    {
        String chunkMsg = "ChunkDataRcv";
        pw.println(chunkMsg);
        pw.flush();

        try
        {
            chunkListStr = (String) in.readObject();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("List of chunks to be downloaded are: " + chunkListStr);

        chunkListStrList = Arrays.asList(chunkListStr.split(" "));

        for (String s : chunkListStrList)
        {
            chunkList.add(Integer.valueOf(s));
        }

        return chunkList;
    }

    public void fileMerge(String fN, int sN) throws IOException
    {
        String file_Name = fN;
        int totalSeg = sN;

        FileInputStream inFile;
        FileOutputStream outFile = new FileOutputStream(file_Name);

        for (int i = 1; i <= totalSeg; i++)
        {
            inFile = new FileInputStream(file_Name + ".part" + i);
            byte[] inBuffer = new byte[(int)(inFile.getChannel().size())];
            inFile.read(inBuffer);
            inFile.close();
            outFile.write(inBuffer);
        }
        outFile.flush();
        outFile.close();
    }
}

class ClientServer extends Thread
{
    private Socket clSocket;
    private String file_name;
    private int segNos;
    private int clNum;
    private int totalSeg;

    String line;
    int upldCl;
    int upldScktNos;
    String file_rqd;
    ServerSocket upldServSckt;
    Socket upldSckt;
    BufferedReader br;


    public ClientServer(String file_name, int clNum, int totalSeg)
    {
        this.file_name = file_name;
        this.clNum = clNum;
        this.totalSeg = totalSeg;
    }

    public void run()
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader("config.txt"));

            while ((line = br.readLine()) != null)
            {
                int temp = Character.getNumericValue(line.charAt(0));

                if (temp == (clNum+1))
                {
                    upldScktNos = Integer.parseInt(line.substring(2, 6));
                    System.out.println("Upload Socket is: " + upldScktNos);
                    upldCl = Character.getNumericValue(line.charAt(9));
                    System.out.println("Upload Neighbor: " + upldCl);
                }
            }

            upldServSckt = new ServerSocket(upldScktNos);
            upldSckt = upldServSckt.accept();
            System.out.println("Server connected to: " + upldSckt.getInetAddress().getHostAddress());

            ObjectOutputStream out = new ObjectOutputStream(upldSckt.getOutputStream());
            PrintWriter pw = new PrintWriter(upldSckt.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(upldSckt.getInputStream());
            br = new BufferedReader(new InputStreamReader(upldSckt.getInputStream()));

            while (true)
            {
                String msgClientChunk = br.readLine();
                System.out.println(msgClientChunk);
                if (msgClientChunk.trim().toLowerCase().equals("chunkdatarcv"))
                {
                    out.writeObject(getChunk());
                }
                if (msgClientChunk.matches("List: (.*)"))
                {
                    file_rqd = msgClientChunk.substring(6);
                    System.out.println("Required file by ClientClient: " + file_rqd);
                    sendFile(file_rqd);
                }
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public String getChunk() throws FileNotFoundException
    {
        String chunkListArray = "";
        try {
            BufferedReader br2 = new BufferedReader(new FileReader("ChunkList.txt"));

            while ((line = br2.readLine()) != null)
            {
                chunkListArray = chunkListArray +line + " ";
            }
        }
        catch (IOException ery)
        {
            ery.printStackTrace();
        }
        return chunkListArray;
    }

    public void sendFile(String file_rqd)
    {
        System.out.println("Inside file sending function.");

        try
        {
            File file = new File(file_rqd);
            long segSize = file.length();
            System.out.println("Segment size is: " + segSize);

            PrintWriter pw = new PrintWriter(upldSckt.getOutputStream(), true);
            System.out.println("Segment been sent: " + file_rqd);


            byte[] bufferByteArray = new byte[(int) segSize];

            InputStream is = new FileInputStream(file);
            int c = is.read(bufferByteArray, 0, (int) segSize);
            System.out.println("read buffer len " + c);

            System.out.println("Segment: " + file_rqd + " -> sending in progress");

            ObjectOutputStream out = new ObjectOutputStream(upldSckt.getOutputStream());
            out.writeObject(bufferByteArray);

            System.out.println("File Sent:" + file_rqd);

        }
        catch (IOException er)
        {
            er.printStackTrace();
        }
    }
}