package me.max.power;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class PowerCommandExecutor implements CommandExecutor {

    private final PowerPlugin plugin;

    // Конструктор принимает ссылку на основной плагин
    public PowerCommandExecutor(PowerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cИспользуйте: /power <игрок> <способность>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден!");
            return true;
        }

        String ability = args[1].toLowerCase();
        if (!ability.equals("kiborg") && !ability.equals("vegetarian") && !ability.equals("fisher") && !ability.equals("automaton")&& !ability.equals("richman")&& !ability.equals("undead")) {
            sender.sendMessage("§cНеверная способность! Доступны: kiborg, vegetarian, fisher, automaton, richman, undead.");
            return true;
        }

        // Назначаем класс игроку
        this.plugin.getPlayerClasses().put(target.getUniqueId(), ability);
        sender.sendMessage("§aВы назначили " + target.getName() + " способность: " + ability);
        target.sendMessage("§aВам назначена способность: " + ability);

        // Устанавливаем здоровье и применяем эффект регенерации
        switch (ability) {
            case "fisher":
                target.setMaxHealth(12.0);
                this.plugin.giveFishingRod(target);
                target.sendMessage("§eВы стали рыбаком! Вечное желение плвать с дельфинами и 12 HP.");
                break;
            case "kiborg":
                target.setMaxHealth(24.0);
                target.sendMessage("§eВы стали киборком ! тепеть не судьба сходить в душ и 24 HP.");
                break;
            case "vegetarian":
                target.setMaxHealth(20.0);
                target.sendMessage("§eВы стали вегоном! теперь отбрости стейк и присоединитесь к коровам .");
                break;
            case "automaton":
                target.setMaxHealth(30.0);
                target.sendMessage("§eВы стали автоматоном! Вечное замедление и 30 HP.");
                break;
            case "richman":
                target.setMaxHealth(30.0);
                target.sendMessage("§eВы стали боготеям! жажда денг вас негогда не покинет вам нужны все алмазы и золота всееее!!!");
                break;
            case "undead":
                target.setMaxHealth(19.0);
                target.sendMessage("§eВы стали нежитью! Мир боится вас, но вы боитесь солнца");
                break;
        
        }

        // Выдаём эффект регенерации максимального уровня на 2 секунды
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 254, false, true));

        return true;
    }
}
