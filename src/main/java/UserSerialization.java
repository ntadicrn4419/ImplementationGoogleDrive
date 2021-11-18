import com.fasterxml.jackson.databind.ObjectMapper;
import storageSpec.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class UserSerialization implements ISerialization {
    private String defaultLocalPath = "";
    @Override
    public void saveUserData(String filePath, AbstractUser user) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UserData userData = new UserData();
            userData.setUserName(user.getUserName());
            userData.setPassword(user.getPassword());
            userData.setStoragesAndPrivileges(user.getStoragesAndPrivileges());

            String json = objectMapper.writeValueAsString(userData);
            java.io.File file = new java.io.File(filePath);
            Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.APPEND);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<UserData> readSavedUsers(String filePath){
        //System.out.println("readSaveUsers :" + filePath);
        List<UserData> myUsers = new ArrayList<>();
        UserData userData;
        try {
            UserManager.getUser().download(filePath,this.defaultLocalPath);
            String [] array = filePath.split("/");
            ObjectMapper objectMapper = new ObjectMapper();
            Scanner scanner = new Scanner(new File(this.defaultLocalPath + "\\" + array[array.length-1]));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.length() == 0)
                    continue;
                userData = objectMapper.readValue(line, UserData.class);
                myUsers.add(userData);
            }
            java.io.File myObj = new java.io.File(this.defaultLocalPath + "\\" + array[array.length-1]);
            myObj.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return myUsers;
    }

    @Override
    public void saveStorageData(String s, Storage storage) {

    }

    @Override
    public StorageData readStorageData(String s) {
        return null;
    }

    public void setDefaultLocalPath(String defaultLocalPath) {
        this.defaultLocalPath = defaultLocalPath;
    }
}