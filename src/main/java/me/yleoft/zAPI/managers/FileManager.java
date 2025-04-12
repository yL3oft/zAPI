package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.utils.FileUtils;
import me.yleoft.zAPI.zAPI;
import java.io.File;

public class FileManager {

    private zAPI zAPI;

    public File df;
    public File lang;
    public File lang2;
    public File fBACKUP = null;
    public FileUtils fuLang;
    public FileUtils fuLang2;
    public FileUtils fuBACKUP = null;

    public FileManager(zAPI zAPI) {
        this.zAPI = zAPI;
        this.df = zAPI.getPlugin().getDataFolder();
        this.lang = new File(this.df, "languages/en.yml");
        this.lang2 = new File(this.df, "languages/pt-br.yml");
        this.fuLang = new FileUtils(zAPI, this.lang, "languages/en.yml");
        this.fuLang2 = new FileUtils(zAPI, this.lang2, "languages/pt-br.yml");

    }

}
