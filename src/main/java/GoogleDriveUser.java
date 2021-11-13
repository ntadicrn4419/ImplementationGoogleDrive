import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;


public class GoogleDriveUser extends AbstractUser {
    /**
     * Connection to the GoogleDrive
     */
    private final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    private Drive driveService;

    static {
        try {
            UserManager.registerUser(new GoogleDriveUser());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public GoogleDriveUser() throws GeneralSecurityException, IOException{
        super.initStoragesAndPrivileges(new HashMap<>());
        driveService = new Drive.Builder(HTTP_TRANSPORT, DriveQuickstart.getJsonFactory(), DriveQuickstart.getCredentials(HTTP_TRANSPORT))
                .setApplicationName(DriveQuickstart.getApplicationName())
                .build();
    }

    /**
     * Creates empty root folder with given storageName at the defined rootLocation on GoogleDrive
     * Created storage is added in map of storages and priveleges.
     */
    @Override
    public void initStorage(String storageName, String rootLocation) {
        if(!checkName(storageName)){
            System.out.println("File with name: " + storageName +  " already exists. Choose another name.");
            return;
        }
        File fileMetadata = new File();
        fileMetadata.setName(storageName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            file.setName(storageName);
            file.setMimeType("application/vnd.google-apps.folder");//dodato
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());

        Storage storage = new Storage(storageName, this, rootLocation, file.getId());
        super.addStorage(storage, Privilege.ADMIN);

    }

    @Override
    public void saveStorageData() {

    }
    /**
     * Creates directory with given name('String dir') at the specified path(String 'path'); for example createDir("newDir" , "storage/dir1/dir2")
     * When it is created, path of our new directory will be "storage/dir1/dir2/newDir"
     * */
    @Override
    public void createDir(String dir, String path) {
        if(!checkName(dir)){
            System.out.println("File with name: " + dir +  " already exists. Choose another name.");
            return;
        }
        File fileMetadata = new File();
        fileMetadata.setName(dir);
        String folderId = this.findParentDirID(path);
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            file.setName(dir);
            file.setMimeType("application/vnd.google-apps.folder");//dodato
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());
    }

    @Override
    public void createDir(String s, String s1, String s2, int i) {

    }
    /**
     * Uploads existing file with given name, with given type(txt, png...), from pathOnMyComputer(location on local file system) to google drive storage  with given path
     * inside storage->for example: "storage/dir1/dir2"
     * When it is created, path of our file will be "storage/dir1/dir2/myFile"
     */
    @Override
    public void uploadExistingFile(String fileName, String pathWhereToCreateFile,  String pathOnMyComputer, String fileType) {
        String folderId = this.findParentDirID(pathWhereToCreateFile);
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(pathOnMyComputer);
        FileContent mediaContent = new FileContent(fileType, filePath);
        File file = null;
        try {
            file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            file.setName(fileName);
            file.setMimeType("application/vnd.google-apps.file");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId() + "; file name: " + file.getName());
    }
    /**
     * Creates file on specified path on local file system and then uploads it on google drive storage with given path inside storage
     */
    @Override
    public void createFile(String fileName, String pathWhereToUploadFile, String pathOnMyComputer, String fileType) {
        Path filepath = Paths.get(pathOnMyComputer); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);     //creates empty file at specified location(path) on local file system
            System.out.println("File Created at Path: "+p);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile(fileName, pathWhereToUploadFile, pathOnMyComputer, fileType);
    }

    @Override
    public void move(Collection<String> collection, String s) {

    }

    @Override
    public void move(String s, String s1) {

    }

    @Override
    public void delete(String name) {
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method delete: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method delete: there is more than 1 file with name: " + name);
            return;
        }
        try {
            this.driveService.files().delete(files.get(0).getId()).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    @Override
    public void download(String name, String whereToDownload) {
        List<File> files = findFileByName(name);
        if(files == null || files.isEmpty()){
            System.out.println("error in method download: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method download: there is more than 1 file with name: " + name);
            return;
        }
        File f  = files.get(0);
        String fileId = f.getId();

        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        FileList childrenList = null;
        try {
            childrenList = driveService.files().list().setQ(fileQuery).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(childrenList.getFiles() == null || childrenList.getFiles().size() == 0){ // ako je file, a ne folder, onda nema dece i mozemo odmah da ga download-ujuemo

            try {
                download1(fileId, whereToDownload, name);
            }catch (Exception e1){
                try {
                    download2(fileId, whereToDownload, name);
                }catch (Exception e2){ // u ovaj catch se udje samo ako se radi o praznom folderu(nema decu, a nije fajl)
                    //e2.printStackTrace();
                    Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
                    try {
                        Path p = Files.createDirectory(filepath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
        //PROBLEMI :
        // 1) downloadovanje praznih foldera(i praznih fajlova)??--> problem je sto ja odredjujem da li je folder po tome da li ima decu, sta ako je prazan folder???
        // 2).png fajl se uspesno skine, ali nece da se prikaze slika(nije podrzan format)
        // 3)kod downlodovanja2 za google docs, sadrzaj fajla nije pregledan, nije citak
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        try {
            Path p = Files.createDirectory(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File file: childrenList.getFiles()){ // ako je folder, znaci ima decu, moramo da download-ujem jedno po jedno dete
            if(isFolder(file.getId())){
                download(file.getName(), whereToDownload + "\\" + name);
            }
            try {
                if(isFolder(file.getId())){//mora da postoji ova provera, zato sto nakon rekurzije, ponovo cemo pokusati da downlodujemo folder, a to ne moze!
                    continue;
                }
                download1(file.getId(), whereToDownload + "\\" + name, file.getName());
            }catch (Exception e1){
                try {
                    download2(file.getId(), whereToDownload + "\\" + name, file.getName());
                }catch (Exception e2){//ovaj catch se desi kada se imamo ugnjezden prazan folder, prazan folder u nekom folderu
                    //System.out.println("problem sa " + file.getName());
                    //e2.printStackTrace();
                    Path fpath = Paths.get(whereToDownload + "\\" + file.getName()); //creates Path instance
                    try {
                        Path p = Files.createDirectory(fpath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    private boolean isFolder(String fileId){
        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        FileList childrenList = null;
        try {
            childrenList = driveService.files().list().setQ(fileQuery).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(childrenList.getFiles() == null || childrenList.getFiles().size() == 0){
            return false;
        }
        return true;
    }
     */
    @Override
    public void download(String name, String whereToDownload) {
        List<File> files = findFileByName(name);
        if(files == null || files.isEmpty()){
            System.out.println("error in method download: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method download: there is more than 1 file with name: " + name);
            return;
        }
        File f  = files.get(0);
        String fileId = f.getId();

        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        FileList childrenList = null;
        try {
            childrenList = driveService.files().list().setQ(fileQuery).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!isFolder(f)){ // ako nije folder, vec fajl, mozemo odmah da ga download-ujuemo
            try {
                download1(fileId, whereToDownload, name);
            }catch (Exception e1){
                try {
                    download2(fileId, whereToDownload, name);
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }
            return;
        }
        //PROBLEMI :
        // 1) downloadovanje praznih foldera(i praznih fajlova)??--> problem je sto ja odredjujem da li je folder po tome da li ima decu, sta ako je prazan folder???
        // 2).png fajl se uspesno skine, ali nece da se prikaze slika(nije podrzan format)
        // 3)kod downlodovanja2 za google docs, sadrzaj fajla nije pregledan, nije citak
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        try {
            Path p = Files.createDirectory(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File file: childrenList.getFiles()){
            if(isFolder(file)){
                download(file.getName(), whereToDownload + "\\" + name);
                continue;
            }
            try {
                download1(file.getId(), whereToDownload + "\\" + name, file.getName());
            }catch (Exception e1){
                try {
                    download2(file.getId(), whereToDownload + "\\" + name, file.getName());
                }catch (Exception e2){
                    System.out.println("problem sa " + file.getName());
                    e2.printStackTrace();
                }
            }
        }

    }
    private boolean isFolder(File file){

        if(file.getMimeType() != null && file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")){
            return true;
        }
        return false;
    }
    private void download1(String fileId, String whereToDownload, String name) throws IOException {//za obicne(npr .txt )fajlove
        OutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        Path p = Files.createFile(filepath);
        List<String> lines = Arrays.asList(outputStream.toString());
        Files.write(Paths.get(whereToDownload + "\\" + name), lines, StandardCharsets.UTF_8);
    }
    private void download2(String fileId, String whereToDownload, String name) throws IOException {//za google docs fajlove
        OutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().export(fileId, "application/zip").executeMediaAndDownloadTo(outputStream);
        outputStream.flush();
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        Path p= Files.createFile(filepath);
        List<String> lines = Arrays.asList(outputStream.toString());
        Files.write(Paths.get(whereToDownload + "\\" + name), lines);
        outputStream.close();
    }
    private List<File> findFileByName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        return files;
    }

    //PROBLEM: ne radi kada se prosledi cela putanja, npr: mojeSkladiste/dir1; radi samo sa dir1-->srediti
    //RESENO: ako korisnik unese celu putanju, mi uzimamo samo poslednji naziv zato sto znamo da su imena jedinstvena na nivou skladista
    @Override
    public Collection<String> searchFilesInDir(String dir) {

        String pathToDir[] = dir.split("/");

        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false and mimeType!='application/vnd.google-apps.folder'";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        for(File f: files){
            fileNames.add(f.getName());
        }
        return fileNames;

    }

    @Override
    public Collection<String> searchDirsInDir(String dir) {
        String pathToDir[] = dir.split("/");

        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false and mimeType='application/vnd.google-apps.folder'";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        for(File f: files){
            fileNames.add(f.getName());
        }
        return fileNames;
    }

    //OVA METODA NEMA SMISLA...
    @Override
    public Collection<String> searchByName(String name) {//iako vraca kolekciju stringova, vratice uvek kolekciju sa samo jednim clanom, zato sto su imena
                                                        // jedinstvena na nivou skladista
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Override
    public Collection<String> searchByExtension(String extention) {
        String pageToken = null;
        ArrayList fileNames = new ArrayList();
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setQ("mimeType='" + extention + "'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                fileNames.add(file.getName());
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return fileNames;
    }

    @Override
    public Collection<String> searchByNameSorted(String s) {
        return null;
    }

    @Override
    public Date getModificationDate(String s) {
        return null;
    }

    @Override
    public Date getCreationDate(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByDateCreationRange(Date date, Date date1) {
        return null;
    }

    @Override
    public Collection<String> searchFilesInDirByDateCreationRange(Date date, Date date1, String s) {
        return null;
    }

    @Override
    public void setStorageSize(int size, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da definise velicinu skladista u bajtovima");
            return;
        }
        storage.setStorageSize(size);
    }

    @Override
    public void setForbiddenExtensions(Collection<String> ext, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da definise koje su zabranjene ekstenzije u skladistu");
            return;
        }
        storage.setForbiddenExtensions(ext);
    }

    @Override
    public void setMaxFileNumberInDir(int i, Storage storage, String dirPath) {

    }

    @Override
    public void addUser(AbstractUser abstractUser, Storage storage, Privilege privilege) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da doda korisnika");
            return;
        }
        storage.addUser(abstractUser);//sluzi da storage zna da ovaj user moze da mu pristupi
        abstractUser.addStorage(storage, privilege);//sluzi da user zna da za ovaj storage ima ovaj nivo privilegije
    }

    @Override
    public void removeUser(AbstractUser abstractUser, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da obrise korisnika");
            return;
        }
        storage.removeUser(abstractUser);
        abstractUser.removeStorage(storage);
    }
    private String findStorageID(String storageName){
        for (Storage storage: super.getStoragesAndPrivileges().keySet()) {
            if(storage.getStorageName().equalsIgnoreCase(storageName)){
                return  storage.getStorageID();
            }
        }
        System.out.println("ERROR: ID for storage with name: " + storageName + " has not been found");
        return null;
    }

    private String findParentDirID(String path){
        String[] array = path.split("/");
        if(array.length == 1){//ako pravimo fajl(folder) direktno u korenksom direktorijumu skladista
            return findStorageID(array[0]);
        }
        String parentName = array[array.length-1];
        String storageName = array[0];
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + parentName + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method findParentDirID: there is no parent with that name");
            return null;
        }
        if(files.size() > 1){
            System.out.println("error in method findParentDirID: there is more than 1 parent with that name");
            return null;
        }
        return files.get(0).getId();
    }
    private boolean checkName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if(file.getName().equalsIgnoreCase(name)){
                    return false;
                }
            }
        }
        return true;
    }
}
