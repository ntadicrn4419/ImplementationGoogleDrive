import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.Drive;
import storageSpec.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class UserSerialization implements ISerialization {
    private String defaultLocalPath = "";
    @Override
    public void saveUserData(String filePath, String userName, String password, Map<String, Privilege> storagesAndPrivileges, boolean append) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UserData userData = new UserData();
            userData.setUserName(userName);
            userData.setPassword(password);
            userData.setStoragesAndPrivileges(storagesAndPrivileges);

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
        List<UserData> myUsers = new ArrayList<>();
        UserData userData;
        FileInputStream inputStream = null;
        try {
            UserManager.getUser().download(filePath,this.defaultLocalPath);

            ObjectMapper objectMapper = new ObjectMapper();

            inputStream = new FileInputStream(this.defaultLocalPath + "\\" + "users.json");
            Scanner scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.length() == 0)
                    continue;
                userData = objectMapper.readValue(line, UserData.class);
                myUsers.add(userData);
            }
            inputStream.close();
            Files.delete(Paths.get(this.defaultLocalPath + "\\" + "users.json"));
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
            storageData.setDirsMaxChildrenCount(storage.getDirsMaxChildrenCount());

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