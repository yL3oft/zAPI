package me.yleoft.zAPI.managers;

import me.yleoft.zAPI.utils.FileUtils;
import me.yleoft.zAPI.zAPI;
import java.io.File;

public class FileManager {

    public File df = zAPI.getInstance().getPlugin().getDataFolder();
    public File lang = new File(this.df, "languages/en.yml");
    public File lang2 = new File(this.df, "languages/pt-br.yml");
    public File fBACKUP = null;
    public FileUtils fuLang = new FileUtils(this.lang, "languages/en.yml");
    public FileUtils fuLang2 = new FileUtils(this.lang2, "languages/pt-br.yml");
    public FileUtils fuBACKUP = null;

}
