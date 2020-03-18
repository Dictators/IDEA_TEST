package fileAanlysis;

import com.baidu.aip.ocr.AipOcr;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class file_analysis{
    private static final Path path = Paths.get("E:\\test\\");
    public static void main(String [] args) throws  InterruptedException{
    Thread filescan=new Thread(new fileScan(path));
    Thread fileanalysis=new Thread(new fileAnalysis());
    fileanalysis.start();
    filescan.start();
    filescan.join();
    fileanalysis.join();
    }
}

class  Data{
    public static final Object lock = new Object();
    public static Queue<String> queueFilePath=new LinkedList<>();
    public static Map<String,String> map_weixin=new HashMap<>();
    public static ArrayList<String> failed=new ArrayList<>();
    public static int n=3;
    public static boolean flag=true;

    public static synchronized void addQueen(String a){
        queueFilePath.add(a);
    }
    public static synchronized void delQueen(){
        queueFilePath.remove();
    }
    public static synchronized String getHeadNot(){
        return queueFilePath.peek();
    }
    public static synchronized String getHeadYes(){
        return queueFilePath.poll();
    }
    public static synchronized boolean isEmpty(){
        return queueFilePath.isEmpty();
    }

    public static synchronized void setMap_weixin(String paths,String wordss){
        map_weixin.put( paths, wordss);
    }
    public static synchronized void delMap_weixin(String paths){
        map_weixin.remove(paths);
    }
}

class fileScan implements  Runnable{
    private Path path;
    public fileScan(Path path){
        this.path=path;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            //给path路径加上文件观察服务
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            while (true) {
                final WatchKey key = watchService.take();
                for (WatchEvent<?> watchEvent : key.pollEvents()) {

                    final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
                    final Path filename = watchEventPath.context();
                    final WatchEvent.Kind<?> kind = watchEvent.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    //创建事件
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        System.out.println("[新建]");
                        synchronized(Data.lock){
                            Data.addQueen(path.toString() + "\\" + filename.toString());
                        }

                    }

                    //删除事件
                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                    }

                }
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException | IOException ex) {
            System.err.println(ex);
        }
        return ;
    }
}

class fileAnalysis implements Runnable{

    @Override
    public void run() {
        final String APP_ID = "18890384";
        final String API_KEY = "y50FGh8HekqhbhmALaWVv3qd";
        final String SECRET_KEY = "Bsyq4MMnqGGmOaPd1ylXDTIvUSdq6cKk";
        while (true){
            AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);
            String path = Data.getHeadNot();
            JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
            String analysisjson=analysisJson(res.toString(2));
            //对分析结果进行判断
            if(analysisjson==null){
                if(Data.n==0){
                    String failedFile=Data.getHeadYes();
                    Data.failed.add(failedFile);
                }
                else {
                    Data.n--;
                    continue;
                }
            }else {
                Data.setMap_weixin(path,analysisjson );
                Data.delQueen();
            }
        }
    }

    public static String analysisJson(String json) {
        JsonObject jsonobject = (JsonObject) new JsonParser().parse(json);
        String wordRes = "";
        try{
            for (int i = 0; i < jsonobject.get("words_result").getAsJsonArray().size(); i++) {
                String wordjson = jsonobject.get("words_result").getAsJsonArray().get(i).toString();
                JsonObject jsonobject2 = (JsonObject) new JsonParser().parse(wordjson);
                wordRes = wordRes + jsonobject2.get("words").getAsString().trim();
            }
        }catch (Exception e){
            return null;
        }
        return wordRes;
    }
}
