package fileAanlysis;

import com.baidu.aip.ocr.AipOcr;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.*;


public class file_main {


    public static Queue<String> queueFilePath=new LinkedList<>();
    public static Map<String,String> map_weixin=new HashMap<>();
    public static ArrayList<String> failed=new ArrayList<>();
    public static int n=3;
    public static boolean flag=true;

    public static void main(String [] args)   {
        final Path path = Paths.get("E:\\test\\");
        getFileList(path);
        //先循环遍历一下已经有的文件
        while (!queueFilePath.isEmpty()){
            fileanalysis(queueFilePath,n);
        }
        fileScaner(path);
    }



    public static void getFileList(Path path) {
        File file = new File(String.valueOf(path));
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                queueFilePath.add(tempList[i].toString());
            }
        }
    }

    public static Path fileScaner(Path path) {

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
                        queueFilePath.add(path.toString() + "\\" + filename.toString());
                        //循环遍历队列，分析队列中的数据
                        while (!queueFilePath.isEmpty()){
                            flag=fileanalysis(queueFilePath,n);
                            if(flag){
                                n=3;
                            }
                            else {
                                n--;
                            }
                        }
                        //输出结果
                        for(String value:map_weixin.values())
                        {
                            System.out.println(value);
                        };
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
        return null;
    }

    public static boolean fileanalysis(Queue<String> Queue_path,int n) {
        final String APP_ID = "18890384";
        final String API_KEY = "y50FGh8HekqhbhmALaWVv3qd";
        final String SECRET_KEY = "Bsyq4MMnqGGmOaPd1ylXDTIvUSdq6cKk";
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        String path = Queue_path.peek();
        JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
        String analysisjson=analysisJson(res.toString(2));
        if(analysisjson==null){
                if(n==0){
                    String failedFile=queueFilePath.remove();
                    failed.add(failedFile);
                    return true;
                }
                else {
                    return false;
                }
        }else {
            map_weixin.put(path,analysisjson );
            queueFilePath.remove();
            return true;
        }
        //遍历一下结果

    }

    public static String analysisJson(String json) {
        JsonObject jsonobject = (JsonObject) new JsonParser().parse(json);
        String wordRes = "";
        try{
            for (int i = 0; i < jsonobject.get("words_result").getAsJsonArray().size(); i++) {
                String wordjson = jsonobject.get("words_result").getAsJsonArray().get(i).toString();
                JsonObject jsonobject2 = (JsonObject) new JsonParser().parse(wordjson);
                wordRes = wordRes + jsonobject2.get("words").getAsString().trim();
                //                System.out.println(word_res);
            }
        }catch (Exception e){
            return null;
        }
                //            System.out.println(word_res);
        return wordRes;
    }
}

