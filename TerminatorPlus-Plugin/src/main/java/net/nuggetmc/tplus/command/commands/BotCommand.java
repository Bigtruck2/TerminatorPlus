package net.nuggetmc.tplus.command.commands;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.legacyagent.EnumTargetGoal;
import net.nuggetmc.tplus.api.agent.legacyagent.LegacyAgent;
import net.nuggetmc.tplus.api.utils.ChatUtils;
import net.nuggetmc.tplus.bot.BotManagerImpl;
import net.nuggetmc.tplus.command.CommandHandler;
import net.nuggetmc.tplus.command.CommandInstance;
import net.nuggetmc.tplus.command.annotation.Arg;
import net.nuggetmc.tplus.command.annotation.Autofill;
import net.nuggetmc.tplus.command.annotation.Command;
import net.nuggetmc.tplus.command.annotation.OptArg;
import net.nuggetmc.tplus.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;

public class BotCommand extends CommandInstance {

    private final TerminatorPlus plugin;
    private final CommandHandler handler;
    private final BotManagerImpl manager;
    private final LegacyAgent agent;
    private final BukkitScheduler scheduler;
    private final DecimalFormat formatter;
    private final Map<String, ItemStack[]> armorTiers;
    private AICommand aiManager;

    public BotCommand(CommandHandler handler, String name, String description, String... aliases) {
        super(handler, name, description, aliases);

        this.handler = commandHandler;
        this.plugin = TerminatorPlus.getInstance();
        this.manager = plugin.getManager();
        this.agent = (LegacyAgent) manager.getAgent();
        this.scheduler = Bukkit.getScheduler();
        this.formatter = new DecimalFormat("0.##");
        this.armorTiers = new HashMap<>();

        this.armorTierSetup();
    }

    @Command
    public void root(CommandSender sender) {
        commandHandler.sendRootInfo(this, sender);
    }
    @Command(
            name = "togglePlace",
            desc = "toggle whether bots can place blocks"
    )
    public void togglePlace() {
        LegacyAgent.canPlace = !LegacyAgent.canPlace;
    }
    @Command(
            name = "toggleMine",
            desc = "toggle whether bots can mine"
    )
    public void toggleMine() {
        LegacyAgent.canMine = !LegacyAgent.canMine;
    }
    @Command(
            name = "create",
            desc = "Create a bot."
    )
    public void create(Player sender, @Arg("name") String name, @OptArg("skin") String skin) {
        manager.createBots(sender, name, skin, 1);
    }

    @Command(
            name = "multi",
            desc = "Create multiple bots at once."
    )
    public void multi(Player sender, @Arg("amount") int amount, @Arg("name") String name, @OptArg("skin") String skin) {
        manager.createBots(sender, name, skin, amount);
    }

    @Command(
            name = "give",
            desc = "Gives a specified item to all bots."
    )
    public void give(CommandSender sender, @Arg("item-name") String itemName) {
        Material type = Material.matchMaterial(itemName);

        if (type == null) {
            sender.sendMessage("The item " + ChatColor.YELLOW + itemName + ChatColor.RESET + " is not valid!");
            return;
        }

        ItemStack item = new ItemStack(type);

        manager.fetch().forEach(bot -> bot.setDefaultItem(item));

        sender.sendMessage("Successfully set the default item to " + ChatColor.YELLOW + item.getType() + ChatColor.RESET + " for all current bots.");
    }

    private void armorTierSetup() {
        armorTiers.put("leather", new ItemStack[]{
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LEATHER_HELMET),
        });

        armorTiers.put("chain", new ItemStack[]{
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET),
        });

        armorTiers.put("gold", new ItemStack[]{
                new ItemStack(Material.GOLDEN_BOOTS),
                new ItemStack(Material.GOLDEN_LEGGINGS),
                new ItemStack(Material.GOLDEN_CHESTPLATE),
                new ItemStack(Material.GOLDEN_HELMET),
        });

        armorTiers.put("iron", new ItemStack[]{
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_HELMET),
        });

        armorTiers.put("diamond", new ItemStack[]{
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_HELMET),
        });

        armorTiers.put("netherite", new ItemStack[]{
                new ItemStack(Material.NETHERITE_BOOTS),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_HELMET),
        });
    }

    @Command(
            name = "armor",
            desc = "Gives all bots an armor set.",
            autofill = "armorAutofill"
    )
    @SuppressWarnings("deprecation")
    public void armor(CommandSender sender, @Arg("armor-tier") String armorTier) {
        String tier = armorTier.toLowerCase();

        if (!armorTiers.containsKey(tier)) {
            sender.sendMessage(ChatColor.YELLOW + tier + ChatColor.RESET + " is not a valid tier!");
            sender.sendMessage("Available tiers: " + ChatColor.YELLOW + String.join(ChatColor.RESET + ", " + ChatColor.YELLOW, armorTiers.keySet()));
            return;
        }

        ItemStack[] armor = armorTiers.get(tier);

        manager.fetch().forEach(bot -> {
            if (bot.getBukkitEntity() instanceof Player) {
                Player botPlayer = (Player) bot.getBukkitEntity();
                botPlayer.getInventory().setArmorContents(armor);
                botPlayer.updateInventory();

                // packet sending to ensure
                bot.setItem(armor[0], EquipmentSlot.FEET);
                bot.setItem(armor[1], EquipmentSlot.LEGS);
                bot.setItem(armor[2], EquipmentSlot.CHEST);
                bot.setItem(armor[3], EquipmentSlot.HEAD);
            }
        });

        sender.sendMessage("Successfully set the armor tier to " + ChatColor.YELLOW + tier + ChatColor.RESET + " for all current bots.");
    }

    @Autofill
    public List<String> armorAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(armorTiers.keySet()) : null;
    }

    @Command(
            name = "info",
            desc = "Information about loaded bots.",
            autofill = "infoAutofill"
    )
    public void info(CommandSender sender, @Arg("bot-name") String name) {
        if (name == null) {
            sender.sendMessage(ChatColor.YELLOW + "Bot GUI coming soon!");
            return;
        }

        sender.sendMessage("Processing request...");

        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                Terminator bot = manager.getFirst(name);

                if (bot == null) {
                    sender.sendMessage("Could not find bot " + ChatColor.GREEN + name + ChatColor.RESET + "!");
                    return;
                }

                /*
                 * time created
                 * current life (how long it has lived for)
                 * health
                 * inventory
                 * current target
                 * current kills
                 * skin
                 * neural network values (network name if loaded, otherwise RANDOM)
                 */

                String botName = bot.getBotName();
                String world = ChatColor.YELLOW + bot.getBukkitEntity().getWorld().getName();
                Location loc = bot.getLocation();
                String strLoc = ChatColor.YELLOW + formatter.format(loc.getBlockX()) + ", " + formatter.format(loc.getBlockY()) + ", " + formatter.format(loc.getBlockZ());
                Vector vel = bot.getVelocity();
                String strVel = ChatColor.AQUA + formatter.format(vel.getX()) + ", " + formatter.format(vel.getY()) + ", " + formatter.format(vel.getZ());

                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GREEN + botName);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "World: " + world);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "Position: " + strLoc);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "Velocity: " + strVel);
                sender.sendMessage(ChatUtils.LINE);
            } catch (Exception e) {
                sender.sendMessage(ChatUtils.EXCEPTION_MESSAGE);
            }
        });
    }

    @Autofill
    public List<String> infoAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? manager.fetchNames() : null;
    }

    @Command(
            name = "reset",
            desc = "Remove all loaded bots."
    )
    public void reset(CommandSender sender) {
        sender.sendMessage("Removing every bot...");
        int size = manager.fetch().size();
        manager.reset();
        sender.sendMessage("Removed " + ChatColor.RED + ChatUtils.NUMBER_FORMAT.format(size) + ChatColor.RESET + " entit" + (size == 1 ? "y" : "ies") + ".");

        if (aiManager == null) {
            this.aiManager = (AICommand) handler.getCommand("ai");
        }

        if (aiManager != null && aiManager.hasActiveSession()) {
            Bukkit.dispatchCommand(sender, "ai stop");
        }
    }

    /*
     * EVENTUALLY, we should make a command parent hierarchy system soon too! (so we don't have to do this crap)
     * basically, in the @Command annotation, you can include a "parent" for the command, so it will be a subcommand under the specified parent
     */
    @Command(
            name = "settings",
            desc = "Make changes to the global configuration file and bot-specific settings.",
            aliases = "options",
            autofill = "settingsAutofill"
    )
    public void settings(CommandSender sender, List<String> args) {
        String arg1 = args.isEmpty() ? null : args.get(0);
        String arg2 = args.size() < 2 ? null : args.get(1);

        String extra = ChatColor.GRAY + " [" + ChatColor.YELLOW + "/bot settings" + ChatColor.GRAY + "]";

        if (arg1 == null || ((!arg1.equalsIgnoreCase("setgoal")) && !arg1.equalsIgnoreCase("mobtarget") && !arg1.equalsIgnoreCase("playertarget"))) {
            sender.sendMessage(ChatUtils.LINE);
            sender.sendMessage(ChatColor.GOLD + "Bot Settings" + extra);
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "setgoal" + ChatUtils.BULLET_FORMATTED + "Set the global bot target selection method.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "mobtarget" + ChatUtils.BULLET_FORMATTED + "Allow all future bots spawned to be targeted by hostile mobs.");
            sender.sendMessage(ChatUtils.LINE);
            return;
        }

        if (arg1.equalsIgnoreCase("setgoal")) {
            EnumTargetGoal goal = EnumTargetGoal.from(arg2 == null ? "" : arg2);

            if (goal == null) {
                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GOLD + "Goal Selection Types" + extra);
                Arrays.stream(EnumTargetGoal.values()).forEach(g -> sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + g.name().replace("_", "").toLowerCase()
                        + ChatUtils.BULLET_FORMATTED + g.description()));
                sender.sendMessage(ChatUtils.LINE);
                return;
            }
            agent.setTargetType(goal);
            sender.sendMessage("The global bot goal has been set to " + ChatColor.BLUE + goal.name() + ChatColor.RESET + ".");
        } else if (arg1.equalsIgnoreCase("mobtarget")) {
            manager.setMobTarget(!manager.isMobTarget());
            sender.sendMessage("Mob targeting is now " + (manager.isMobTarget() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.RESET + ". (for all future bots)");
        } else if (arg1.equalsIgnoreCase("playertarget")) {
            if (args.size() < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a player name!");
                return;
            }
            String playerName = args.get(1);
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Could not find player " + ChatColor.YELLOW + playerName + ChatColor.RED + "!");
                return;
            }
            for (Terminator fetch : manager.fetch()) {
                fetch.setTargetPlayer(player.getUniqueId());
            }
            sender.sendMessage("All spawned bots are now set to target " + ChatColor.BLUE + player.getName() + ChatColor.RESET + ". They will target the closest player if they can't be found.\nYou may need to set the goal to PLAYER.");
        }
    }

    @Autofill
    public List<String> settingsAutofill(CommandSender sender, String[] args) {
        List<String> output = new ArrayList<>();

        // More settings:
        // setitem
        // tpall
        // tprandom
        // hidenametags or nametags <show/hide>
        // sitall
        // lookall

        if (args.length == 2) {
            output.add("setgoal");
            output.add("mobtarget");
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("setgoal")) {
                Arrays.stream(EnumTargetGoal.values()).forEach(goal -> output.add(goal.name().replace("_", "").toLowerCase()));
            }
            if (args[1].equalsIgnoreCase("mobtarget")) {
                output.add("true");
                output.add("false");
            }
        }

        return output;
    }

    @Command(
            name = "debug",
            desc = "Debug plugin code.",
            visible = false
    )
    public void debug(CommandSender sender, @Arg("expression") String expression) {
        new Debugger(sender).execute(expression);
    }
}
