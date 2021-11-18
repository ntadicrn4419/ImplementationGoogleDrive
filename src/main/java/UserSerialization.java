import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.Drive;
import storageSpec.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class UserSerialization implements ISerialization {
    private String defaultLocalPath = "";
    @Override
    public void saveUserData(String filePath, AbstractUser user, boolean append) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UserData userData = new UserData();
            userData.setUserName(user.getUserName());
            userData.setPassword(user.getPassword());
            userData.setStoragesAndPrivileges(user.getStoragesAndPrivileges());

            String json = objectMapper.writeValueAsString(userData);
            java.io.File file = new java.io.File(filePath);
            if(append){
                Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.APPEND);
            }else{
                Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.CREATE);
            }


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
            File file = new File(this.defaultLocalPath + "\\" + array[array.length-1]);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.length() == 0)
                    continue;
                userData = objectMapper.readValue(line, UserData.class);
                myUsers.add(userData);
            }
            file.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return myUsers;
    }

    @Override
    public void saveStorageData(String fileWhereToSave, Storage storage) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            StorageData storageData = new StorageData();

            storageData.setStorageName(storage.getStorageName());
            storageData.setStorageID(storage.getStorageID());
            storageData.setStorageSize(storage.getStorageSize());
            storageData.setForbiddenExtensions(storage.getForbiddenExtensions());
            storageData.setRootLocation(storage.getRootLocation());

            String json = objectMapper.writeValueAsString(storageData);
            File file = new File(fileWhereToSave);
            Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.CREATE);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public StorageData readStorageData(String fileFromWhereToRead) {
        UserManager.getUser().download(fileFromWhereToRead, this.defaultLocalPath);
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File(this.defaultLocalPath + "\\" + "storage.json");
        StorageData storageData = null;
        try {
            storageData = objectMapper.readValue(jsonFile, StorageData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        jsonFile.delete();
        return storageData;
    }

    public void setDefaultLocalPath(String defaultLocalPath) {
        this.defaultLocalPath = defaultLocalPath;
    }

}