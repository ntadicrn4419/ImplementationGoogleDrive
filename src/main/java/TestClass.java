import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.*;

public class TestClass {//delete this class later
    public static void main(String... args) throws IOException, GeneralSecurityException {
        try {
            Class.forName("GoogleDriveUser");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //AbstractUser user = UserManager.getUser("ntadic4419rn","TadicPassword", null);
        ISerialization serializator = UserManager.getUserSerializator();
        //serializator.saveUserData("users.json", user);
        String[] credentials = login();
        List<UserData> list = serializator.readSavedUsers("users.json");
        for(UserData ud: list){
            if(ud.getUserName().equalsIgnoreCase(credentials[0]) && ud.getPassword().equalsIgnoreCase(credentials[1])){
                System.out.println("Uspesno ste se ulogovali");
            }
        }

        //user.initStorage("myNewstorageH", "drive");


    }
    public static String[] login(){
        Scanner sc = new Scanner(System.in);
        System.out.println("Unesite username: ");
        String username = sc.nextLine();
        System.out.println("Unesite password: ");
        String password = sc.nextLine();
        String credentials[] = {username, password};
        return credentials;
    }
    /*
    public void saveUserData(String filePath, AbstractUser user) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UserSerialization us = new UserSerialization();
            us.setUserName(user.getUserName());
            us.setPassword(user.getPassword());
            us.setStoragesAndPrivileges(user.getStoragesAndPrivileges());

            String json = objectMapper.writeValueAsString(us);
            java.io.File file = new java.io.File(filePath);
            Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.APPEND);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static List<UserSerialization> readSavedUsers(String filePath){
        List<UserSerialization> myUsers = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            UserSerialization us;
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                us = objectMapper.readValue(line, UserSerialization.class);
                myUsers.add(us);
            }
            for(UserSerialization u: myUsers){
                System.out.println(u.getUserName() + ", " + u.getPassword());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return myUsers;
    }
    */
}
