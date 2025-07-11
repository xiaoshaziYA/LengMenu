package shaziawa.LengMenu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;

public class LengMenu extends JavaPlugin implements Listener {

    private ItemStack menuItem;

    @Override
    public void onEnable() {
        getLogger().info("LengMenu插件已启用！");
        saveDefaultConfig();
        loadConfig();
        
        // 在主线程注册事件和命令
        Bukkit.getScheduler().runTask(this, () -> {
            getServer().getPluginManager().registerEvents(this, this);
            
            // 安全注册命令
            PluginCommand command = getCommand("lengmenu");
            if (command != null) {
                command.setExecutor(new LengMenuCommandExecutor(this));
            } else {
                getLogger().severe("无法注册/lengmenu命令！");
            }
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("LengMenu插件已禁用！");
    }

    // 单独的命令执行器类
    private static class LengMenuCommandExecutor implements CommandExecutor {
        private final LengMenu plugin;
        
        public LengMenuCommandExecutor(LengMenu plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length > 0 && args[0].equalsIgnoreCase("get")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    
                    // 检查是否已有物品
                    if (plugin.hasMenuItem(player)) {
                        player.sendMessage("§c你已经有LengMenu物品了！");
                        return true;
                    }
                    
                    if (player.getInventory().addItem(plugin.menuItem).isEmpty()) {
                        player.sendMessage("§a已获取LengMenu物品！");
                    } else {
                        player.sendMessage("§c你的背包已满，无法获取物品！");
                    }
                } else {
                    sender.sendMessage("§c只有玩家才能使用此命令！");
                }
                return true;
            }
            sender.sendMessage("§b使用: /lengmenu get");
            return true;
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        String materialName = config.getString("item", "CLOCK").toUpperCase().replace("MINECRAFT:", "");
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            getLogger().severe("配置文件中指定的物品无效: " + materialName);
            getLogger().severe("使用默认物品 CLOCK。");
            material = Material.CLOCK;
        }

        List<String> lore = config.getStringList("lore");

        menuItem = new ItemStack(material);
        ItemMeta meta = menuItem.getItemMeta();
        meta.setDisplayName("§6LengMenu物品");
        List<String> loreList = new ArrayList<>();
        for (String loreLine : lore) {
            loreList.add(loreLine.replace("&", "§"));
        }
        meta.setLore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        menuItem.setItemMeta(meta);
    }

    private boolean hasMenuItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(menuItem)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟检查防止并发问题
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (hasMenuItem(player)) {
                return;
            }
            
            if (player.getInventory().addItem(menuItem).isEmpty()) {
                player.sendMessage("§a你已自动获得LengMenu物品！");
            } else {
                player.sendMessage("§c你的背包已满，无法自动获取物品！");
                player.sendMessage("§c你可以使用 §b/lengmenu get §c再获取一个。");
            }
        }, 20L); // 延迟1秒执行
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(menuItem)) {
            if (event.getSlotType() == SlotType.OUTSIDE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().isSimilar(menuItem)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                
                FileConfiguration config = getConfig();
                List<String> commands = config.getStringList("command");
                
                if (commands != null && !commands.isEmpty()) {
                    for (String command : commands) {
                        command = command.trim();
                        if (command.startsWith("[player]")) {
                            String cmd = command.substring(8).trim();
                            event.getPlayer().performCommand(cmd);
                        } else if (command.startsWith("[console]")) {
                            String cmd = command.substring(9).trim();
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }
                    }
                }
            }
        }
    }
}