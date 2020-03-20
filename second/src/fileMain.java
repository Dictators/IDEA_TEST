import com.baidu.aip.ocr.AipOcr;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.rtf.RtfWriter2;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class fileMain {
    /**
     * 文件的路径
     */
    private static  Path path=null;
    /**
     * 获取命令行参数，并将参数传送给路径
     */
    public static String wx="";
    public static String qq="";
    /**
     * 文档的输出路径
     */
    public static String outPutPath;
    /**
     * 调用aip分析时的路径队列
     */
    public static Queue<String> queueFilePath=new LinkedList<>();
    /**
     * 文件扫描的路径
     */
    private static  Path path_qq = Paths.get(qq);
    private static  Path path_weixin = Paths.get(wx);
    /**
     * 用来存放失败的文件路径
     */
    public static List<String> filePath=new ArrayList<>();
    public static List<String> failed=new ArrayList<>();
    /**
     * false使用微信的解析方法，true使用qq的解析方法
     */
    public static boolean analysisFlag=false;
    /**
     * aip分析失败的文件循环的次数
     */
    public static int n=3;


    /**
     * 用来存放从图片上分析的文件的结果
     */
    public static Map<String,List<String>> map_WX=new HashMap<>();
    public static Map<String,String> map_QQ=new HashMap<>();


    /**
     * 数据库的相应配置
     */
    public static  String sql="INSERT INTO  data  VALUES (?)";
    private static String userName="root";
    private static String password="root";
    private static String url="jdbc:mysql://localhost:3306/user?characterEncoding=utf8&useUnicode=true&useSSL=false&serverTimezone=GMT";

    /**
     * 主方法
     * @param args
     * @throws IOException
     * @throws DocumentException
     */
    public static void main(String [] args) throws IOException, DocumentException {
        Print();
        System.out.println("***************************************************程序开始运行*************************************************");
        if(args.length!=3){
            System.out.println("参数输入错误");
            System.out.println("示例：java -jar Test    参数1(QQ截图路径)   参数2(微信截图路径)  参数3(文档输出路径)");
            return;
        }else{
            qq=args[0];
            wx=args[1];
            outPutPath=args[2];
            path_weixin = Paths.get(wx);
            path_qq=Paths.get(qq);
        }


        path=path_weixin;
        analysisFlag=false;
        Run();
        database();
        path=path_qq;
        analysisFlag=true;
        Run();
        //database();
        Photo();
    }


    public static void Run(){
        getFileList(path);
        while(!queueFilePath.isEmpty()){
            getResult();
        }

        System.out.println("Faied"+failed);
    }
    //链接数据库
    public static void database(){
    try{
        Class.forName("com.mysql.cj.jdbc.Driver");
        System.out.println("MySql驱动加载成功");
    }catch (ClassNotFoundException e){
        System.out.println("加载失败");
    }
    try{
        Connection conn= DriverManager.getConnection(url,userName,password);
        System.out.println("数据库链接成功");
        PreparedStatement ps=null;
        try{
            ps=conn.prepareStatement(sql);
            for(List<String> strList: map_WX.values()){
                String values="";
                for(int k=0;k<strList.size();k++){
                    values=values+strList.get(k);
                }
                ps.setString(1,values);
                int len=ps.executeUpdate();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }catch (SQLException e){
        e.printStackTrace();
    }

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
        System.out.println(res.toString(2));
        if(analysisFlag){
            String analysisjson=analysisJsonToString(res.toString(2));
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
                map_QQ.put(path,analysisjson );
                if(!queueFilePath.isEmpty()){
                    queueFilePath.remove();
                }
            }
        }else{
            List<String> analysisjson=analysisJsonToArray(res.toString(2));
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
                map_WX.put(path,analysisjson );
                if(!queueFilePath.isEmpty()){
                    queueFilePath.remove();
                }
            }
        }

    }

    public static String analysisJsonToString(String json) {
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

    public static List<String> analysisJsonToArray(String json) {
        JsonObject jsonobject = (JsonObject) new JsonParser().parse(json);
        ArrayList<String> wordResArray=new ArrayList<>();
        try{
            for (int i = 0; i < jsonobject.get("words_result").getAsJsonArray().size(); i++) {
                String wordjson = jsonobject.get("words_result").getAsJsonArray().get(i).toString();
                JsonObject jsonobject2 = (JsonObject) new JsonParser().parse(wordjson);
                wordResArray.add(jsonobject2.get("words").getAsString().trim());
            }
        }catch (Exception e){
            return null;
        }
        System.out.println(wordResArray);
        return wordResArray;
    }

    //合成图片
    public static void Photo() throws IOException, DocumentException {
        while (true) {
            {

                String mapvalue = null;
                String firstString = null;
                String url = null;
                String values = null;
                String a = null;
                Map<String, String> hashMap = new HashMap<>();

                String mapkey = null;
                for (Map.Entry<String, String> entry : map_QQ.entrySet()) {

                    mapkey = entry.getKey();
                    mapvalue = entry.getValue();
                    Pattern pt = compile("\\d{2,}");
                    Matcher m = pt.matcher(mapvalue);
                    int i = 0;
                    while (m.find()) {
                        String wxText = m.group();
                        i++;
                        break;
                    }
                    String wxText = mapvalue;
                    for(Map.Entry<String,String> Entry:map_QQ.entrySet()){
                        String qqText = Entry.getValue();

                        int indexOf = wxText.indexOf(qqText);
                        if (indexOf == -1) {
                            //没有匹配上的Map集合
                            Map<String, String> hashMap1 = new HashMap<>();
//              hashMap1.put(mapkey,mapvalue);
                            String noKey = null;
                            String noValue = null;
                            for (Map.Entry<String, String> entry2 : hashMap1.entrySet()) {
                                noKey = entry2.getKey();
                                noValue = entry2.getValue();
                            }
                            hashMap.put(noKey, noValue);
                        } else {
                            //匹配上的Map集合
                            Map<String, String> hashMap2 = new HashMap<>();
                            hashMap2.put(mapkey, mapvalue);
                            for (Map.Entry<String, String> entry1 : hashMap2.entrySet()) {
                                url = entry.getKey();
                                values = entry1.getValue();
                                System.out.println(values);
                                System.out.println(url);
                            }
                        }
                    }

                    a = mapvalue.substring(0, 4);
                    // 设置纸张大小
                    Document document = new Document(PageSize.A4);

                    // 建立一个书写器(Writer)与document对象关联，通过书写器(Writer)可以将文档写入到磁盘中
                    // ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    File file = new File(outPutPath + a + "." + "doc");

                    RtfWriter2.getInstance(document, new FileOutputStream(file));

                    document.open();

                    // 设置中文字体

                    BaseFont bfChinese = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

                    // 标题字体风格

                    Font titleFont = new Font(bfChinese, 12, Font.BOLD);

                    // // 正文字体风格
                    Font contextFont = new Font(bfChinese, 10, Font.NORMAL);

                    Paragraph title = new Paragraph("统计报告");
                    //
                    // 设置标题格式对齐方式

                    title.setAlignment(Element.ALIGN_CENTER);

                    // title.setFont(titleFont);

                    document.add(title);

                    String contextString = mapvalue;
//                         "iText是一个能够快速产生PDF文件的java类库。"
//
//                + " \n"// 换行 + "iText的java类对于那些要产生包含文本，"
//
//                + "表格，图形的只读文档是很有用的。它的类库尤其与java Servlet有很好的给合。"
//
//                + "使用iText与PDF能够使你正确的控制Servlet的输出。";

                    Paragraph context = new Paragraph(contextString);

                    // 正文格式左对齐

                    context.setAlignment(Element.ALIGN_LEFT);

                    // context.setFont(contextFont);

                    // 离上一段落（标题）空的行数

                    context.setSpacingBefore(5);

                    // 设置第一行空的列数

                    context.setFirstLineIndent(20);

                    document.add(context);
                    //
                    // // 利用类FontFactory结合Font和Color可以设置各种各样字体样式
                    //
                    // Paragraph underline = new Paragraph("下划线的实现", FontFactory.getFont(
                    // FontFactory.HELVETICA_BOLDOBLIQUE, 18, Font.UNDERLINE,
                    // new Color(0, 0, 255)));
                    //
                    // document.add(underline);
                    //

                    // // 添加图片 Image.getInstance即可以放路径又可以放二进制字节流
                    //

                    try {
                        Image img = Image.getInstance(url);
                        img.setAbsolutePosition(0, 0);

                        img.setAlignment(Image.LEFT);// 设置图片显示位置
                        document.add(img);

                        document.close();

                    } catch (FileNotFoundException E) {
                        System.out.println("没有找到图片路径。。。。。" + mapkey);
                    }
                    //
                    // img.scaleAbsolute(60, 60);// 直接设定显示尺寸
                    //
                    // // img.scalePercent(50);//表示显示的大小为原尺寸的50%
                    //
                    // // img.scalePercent(25, 12);//图像高宽的显示比例
                    //
                    // // img.setRotation(30);//图像旋转一定角度
                    //


                    // 得到输入流
                    // wordFile = new ByteArrayInputStream(baos.toByteArray());
                    // baos.close();
//        return "成功";
//              Thread    thread=new Thread();
//              @Override
//                      public void run(){
//                  hashMap.remove(url);
//              }
                    //        Iterator<String> iterator=hashMap.keySet().iterator();
//              while (iterator.hasNext()){
//                  String  key=iterator.next();
//                  if(url.equals(key)){
//                      iterator.remove();
//                  }
//            }
                }
            }
        }
    }



    public static void Print(){
        System.out.println("######################################################################################################################################$\n" +
                "######################################################################################################################################$\n" +
                "######################################################################################################################################$\n" +
                "################################################################@@@###################################################################$\n" +
                "########################################################$!'                   `:|&####################################################$\n" +
                "####################################################%`                             '$#################################################$\n" +
                "###################################################!                                .|################################################$\n" +
                "##################################################$`                                 .%###############################################$\n" +
                "##################################################;                           ..      :@##############################################$\n" +
                "#################################################&'  :%!;!%@###|.        ;@##@$!;!%;  `$##############################################$\n" +
                "#################################################%.       .;$%%##!     !##$%&!`     ...%##############################################$\n" +
                "#################################################|           '$&!`     `!$$'           |##############################################$\n" +
                "#################################################!       ..    `|;     ;|.    .`.      !##############################################$\n" +
                "################%:%##############################;  .!########%'!@!      `|@#######%.  ;###############################%&#############$\n" +
                "################&' !#############################; '&#@&&$$$$%!'!#|      .;;:::;!!;:`':|#############################$`;##############$\n" +
                "#################|  '&###########################!              |#|                    ;###########################@; `$##############$\n" +
                "##################!   ;@#########################&'            .%#|                    |##########################|.  |###############$\n" +
                "##################@;    !#########################@:           :@#|                   :@########################$`   ;################$\n" +
                "###################@:    .%#######################%%$:    ...'$###!     ::          :|$#######################@;    !#################$\n" +
                "#####################;     '&#####################&!|%$%.   .%$:|&'      .`   .`!@$$%|@######################!     |##################$\n" +
                "######################|      ;@####################%';!$@;     .|%.            ;@|:!;$#####################$`    .|###################$\n" +
                "#######################&'      ;@###################$':|%###$:`'%#@|!%&%`   '%##!;|'%####################&:     .|####################$\n" +
                "#########################;       !###################&'`%!'|@#####$`.|#######&;:$|`|###################@;      `$#####################$\n" +
                "##########################%.      .|##################@:.|#!.   ..':;!!;'`     ;:'$##################@;       :@######################$\n" +
                "###########################@:        |##################! !#%''`.            .:`:@#################$'       .%########################$\n" +
                "#############################$`        '$################%`;;     !###!     `'.!#################!         ;@#########################$\n" +
                "###############################%.         ;@##############&:'`    ;##$`      `%###############%`         !@###########################$\n" +
                "#################################|.         `%##############|    '&#@@;     ;@#############@:          !##############################$\n" +
                "###################################%.          ;@############%`  `$###;   `%#############!          .|################################$\n" +
                "#####################################&'          .%############|. !##$` `%############%.          `%##################################$\n" +
                "########################################!      ':.  :&#############################%`           :&####################################$\n" +
                "##########################################|.     ;$;   :&#######################%`  `%&:     `%#######################################$\n" +
                "############################################@;     `%$'   :&################&;   '$#%`    `%##########################################$\n" +
                "###############################################@;     :&&;   '%@########$:   .!@@;     `%#############################################$\n" +
                "################################$`  .%############@!     `%#%'    ;&#@!   '$#$'     `%################################################$\n" +
                "################################$'   `$##############@!.     '$@$:    '%###!.    :&##############%`  '$###############################$\n" +
                "##################################@;  ;###############@$@$'      '%#@;     `|@#################@:    '%###############################$\n" +
                "####################################!  ;@####&%;`         !##$:       :$@%'      .;%@##########!   :@#################################$\n" +
                "######################$'  '$#########;   :&&:      .:%@#@%:.  .!@@|`      .;&#$:        .:;$##;   !###################################$\n" +
                "#####################%.   ;$'       `$$'    `$###@%;.        `!&######&!`      .;&#&|:'`:%@@!   .%#########$'`;$######################$\n" +
                "#####################&'   .|$`       `$#|.  '&!      `!%@###################$!`     .:%##;     ;@|.       '%:   ;@####################$\n" +
                "#######################%'`|@%&##########$`  :@######################################&|;'|@:  .%#|        `%;    :@####################$\n" +
                "##########################$%@############!   :@##########################################@:  '&##########&&$' .|######################$\n" +
                "##########################################%.   `;;''%###################################%`  .%###########@!:$#########################$\n" +
                "############################################@;      |############################|         !@#########################################$\n" +
                "##################################################################################&;. `:%#############################################$\n" +
                "######################################################################################################################################$\n" +
                "######################################################################################################################################$\n" +
                "######################################################################################################################################$\n" +
                "######################################################################################################################################$\n");
    }


}