import com.fasterxml.jackson.databind.ObjectMapper;
import storageSpec.AbstractUser;
import storageSpec.ISerialization;
import storageSpec.Privilege;
import storageSpec.UserData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class UserSerialization implements ISerialization {

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
        List<UserData> myUsers = new ArrayList<>();
        UserData userData;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                userData = objectMapper.readValue(line, UserData.class);
                myUsers.add(userData);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return myUsers;
    }
}