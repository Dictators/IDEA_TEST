package fileAanlysis;

import com.baidu.aip.ocr.AipOcr;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class file_main{
    public static Map<String,String> map_weixin=new HashMap<>();
    public static Queue<String> queueFilePath=new LinkedList<>();
    private static final Path path = Paths.get("E:\\test\\");
    public static List<String> filePath=new ArrayList<>();
    public static List<String> failed=new ArrayList<>();
    public static int n=3;
    public static void main(String [] args){
        getFileList(path);
        while(!queueFilePath.isEmpty()){
            getResult();
            }
        for(String map: map_weixin.values()){
            System.out.println(map);
        }
        System.out.println("Faied"+failed);
    }

    public static void getFileList(Path path) {
        File file = new File(String.valueOf(path));
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()&&filePath.contains(tempList[i].toString())) {
                System.out.println(tempList[i].toString());
                continue;
            }
            else{
                System.out.println(tempList[i].toString());
                filePath.add(tempList[i].toString());
                queueFilePath.add(tempList[i].toString());
            }
        }
    }

    public static void getResult() {
        final String APP_ID = "18890384";
        final String API_KEY = "y50FGh8HekqhbhmALaWVv3qd";
        final String SECRET_KEY = "Bsyq4MMnqGGmOaPd1ylXDTIvUSdq6cKk";
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        String path = queueFilePath.peek();
        JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
        String analysisjson=analysisJson(res.toString(2));
        //对分析结果进行判断
        if(analysisjson==null){
            if(n==0){
                String failedFile=queueFilePath.peek();
                failed.add(failedFile);
                n=3;
            }
            else {
                n--;
            }
        }
        else {
            map_weixin.put(path,analysisjson );
            if(!queueFilePath.isEmpty()){
                queueFilePath.remove();
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