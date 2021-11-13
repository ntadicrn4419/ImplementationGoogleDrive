import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import storageSpec.AbstractUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class TestClass {//delete this class later
    public static void main(String... args) throws IOException, GeneralSecurityException {
          AbstractUser user = new GoogleDriveUser();
//
          ArrayList<String> a = new ArrayList<>();
//          user.initStorage("ms", "drive");
//          user.uploadExistingFile("3b", "ms", "C:\\Users\\tadic\\Desktop\\3b.txt","/txt" );

          a = (ArrayList<String>) user.searchByExtension("application/vnd.google-apps.folder");
            for(String s: a){
                System.out.println(s);
            }
//          user.initStorage("mojeSkladiste", "drive");
//          user.createDir("dir1", "mojeSkladiste");
//          user.uploadExistingFile("3b", "mojeSkladiste/dir1", "C:\\Users\\tadic\\Desktop\\3b.txt","/txt" );
//          user.download("mojeSkladiste", "C:\\Users\\tadic\\Downloads");

//        user.delete("bzvz");
//        user.setUserName("ntadic");
//        user.setPassword("mypass");
//        user.initStorage("skladisteTest", "drive");
//
//        user.createFile("fileName", "skladisteTest", "C:\\Users\\tadic\\Desktop\\novoKreirianFajl.txt", "/txt");
//
//        user.createDir("dir1", "skladisteTest");
//        user.createDir("dir2", "skladisteTest/dir1");
//        user.createDir("dir3", "skladisteTest");
//        user.createDir("dir4", "skladisteTest/dir1/dir2");
//        user.uploadExistingFile("mojNoviFajl","skladisteTest/dir1/dir2/dir4" ,"C:\\Users\\tadic\\Desktop\\aaa.txt","/txt");

    }
}
