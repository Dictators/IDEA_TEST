import java.io.File;

public class Main {
    public static void main(String[] args) {
        File test=new File("e:/test/");
        File f1=new File("e:/test/wakaka.txt");
        test.renameTo(f1);
//        for (String s : test.list()) {
////            System.out.println(s);
////        }
        System.out.println(test.list());
        System.out.println(test.length());
        System.out.println(test.lastModified());
    }
}
