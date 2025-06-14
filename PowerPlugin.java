package me.max.power;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.LivingEntity;
import me.max.power.PowerCommandExecutor;

import java.util.*;

public class PowerPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, String> playerClasses = new HashMap<>();
    private final Map<UUID, List<Long>> attackTimestamps = new HashMap<>();
    private final Map<UUID, Long> vampCooldown = new HashMap<>();
    private final long vampCooldownMillis = 4000;
    private final Map<Location, UUID> illusoryOres = new HashMap<>();
    private final Map<UUID, Long> kiborgCooldown = new HashMap<>();
    private final Map<UUID, Long> lastEatMessage = new HashMap<>();
    private final List<Material> validFishFoods = Arrays.asList(
            Material.COD, Material.COOKED_COD, Material.SALMON, Material.COOKED_SALMON,
            Material.PUFFERFISH, Material.TROPICAL_FISH, Material.DRIED_KELP
    );
    private final Map<UUID, Double> playerWealth = new HashMap<>();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("power").setExecutor(new PowerCommandExecutor(this));

        applyAutoHeal(); // Запускаем систему самовосстановления!
        startWealthCheck();
        startRichManMurmur();
        startIllusoryOreSpawn();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String pClass = playerClasses.get(player.getUniqueId());
                if (pClass != null) {
                    // Киборг: замедление в воде и во время шторма
                    if (pClass.equals("kiborg")) {
                        if (player.getLocation().getBlock().isLiquid() || player.getWorld().hasStorm()) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 1, false, false));

                            player.sendMessage("§cКиборги не созданы для плавания!");
                        }
                    }
                    // Рыбак: получает Dolphin's Grace постоянно
                    if (pClass.equals("fisher")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false));
                    }
                    // Автоматон: получает вечное замедление I
                    if (pClass.equals("automaton")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                    }
                    // боготей: вечная одержимасть деньгамимм
                    if (pClass.equals("richman")) {
                    
                    }
                    if (pClass.equals("undead")) {
                       
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("test")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cТолько игрок может использовать эту команду.");
                return true;
            }

            Player player = (Player) sender;
             if (args.length == 1 && args[0].equalsIgnoreCase("me")) {
            double health = player.getHealth();
            int hunger = player.getFoodLevel();
            float saturation = player.getSaturation();
            Location loc = player.getLocation();
            String classType = playerClasses.getOrDefault(player.getUniqueId(), "Не определён");

            // 🔹 Показываем значение жадности, если игрок богатей
            double greed = playerWealth.getOrDefault(player.getUniqueId(), 0.0);




            player.sendMessage("§7=== §aСтатистика игрока §7===");
            player.sendMessage("§6ХП: §e" + health);
            player.sendMessage("§6Голод: §e" + hunger);
            player.sendMessage("§6Насыщение: §e" + saturation);
            player.sendMessage("§6Координаты: §eX: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ());
            player.sendMessage("§6Класс: §e" + classType);

            // 🔹 Добавляем доп. показатели для богатых и киборгов
            if (greed > 0) {
                player.sendMessage("§6Жадность: §e" + greed);
            }

            return true;
        }

            if (!player.isOp()) {
                player.sendMessage("§cУ вас нет прав на использование этой команды.");
                return true;
            }

            if (args.length < 4) {
                player.sendMessage("§cИспользование: /test illusoryOres <время> <x> <y> <z>");
                return true;
            }

            int duration;
            try {
                duration = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВремя должно быть числом!");
                return true;
            }

            if (duration == 0) duration = Integer.MAX_VALUE; // 🔹 Вечная руда

            double x, y, z;
            try {
                x = Double.parseDouble(args[2]);
                y = Double.parseDouble(args[3]);
                z = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cКоординаты должны быть числами!");
                return true;
            }

            Location loc = new Location(player.getWorld(), x, y, z);
            spawnIllusoryOre(player, loc, duration);

            player.sendMessage("§aИллюзорная руда создана на координатах " + x + ", " + y + ", " + z);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String pClass = playerClasses.get(player.getUniqueId());

        if (pClass == null) return;

        if (pClass.equals("fisher") && player.getLocation().getBlock().getType() == Material.MAGMA_BLOCK) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
        }
        if (pClass.equals("automaton")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
        }
    }
    public void startUndeadEffects() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String pClass = playerClasses.get(player.getUniqueId());
                if ("undead".equals(pClass)) {

                    // 🔹 Горение на солнце
                    if (player.getWorld().getTime() > 23000 || player.getWorld().getTime() < 13000) {
                        if (player.getWorld().getHighestBlockAt(player.getLocation()).getType() == Material.AIR) {
                            player.setFireTicks(100);
                        }
                    }

                    // 🔹 Если ХП < 10, выдаём Силу 1
                    if (player.getHealth() < 10) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, false));
                    }

                    // 🔹 Инверсия эффектов (как у зомби)
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        reverseEffect(player, effect);
                    }
                }
            }
        }, 200L, 400L); // 🔹 Каждые 10-20 сек
    }
    @EventHandler
    public void onUndeadBurnInSun(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playerClasses.getOrDefault(uuid, "").equals("Undead")) return;

        if (shouldBurnInSunlight(player)) {
            if (player.getFireTicks() <= 0) {
                player.setFireTicks(80); // 4 секунды горения
                player.sendMessage("§4Солнце разрывает твою плоть!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 1, 0.8f);
            }
        }
    }
    @EventHandler
    public void onUndeadAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!isUndead(player)) return;
        if (victim.isDead()) return;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (vampCooldown.containsKey(uuid) && now - vampCooldown.get(uuid) < vampCooldownMillis) return;

        double heal = isNight(player) ? 2.5 : 1.0;
        double newHp = Math.min(player.getHealth() + heal, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setHealth(newHp);

        vampCooldown.put(uuid, now);

        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1.4, 0), 6);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SOUL_ESCAPE, 0.7f, 0.6f);
    }

    private boolean isNight(Player player) {
        if ("undead".equals(pClass)){
        long time = player.getWorld().getTime();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        return time > 13000 || time < 1000;
        }
    }


    private void reverseEffect(Player player, PotionEffect effect) {
        PotionEffectType type = effect.getType();
        if (type == PotionEffectType.SLOWNESS) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, effect.getDuration(), effect.getAmplifier()));
        } else if (type == PotionEffectType.WEAKNESS) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, effect.getDuration(), effect.getAmplifier()));
        } else if (type == PotionEffectType.BLINDNESS) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, effect.getDuration(), effect.getAmplifier()));
        } else if (type == PotionEffectType.POISON) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, effect.getDuration(), effect.getAmplifier()));
        } else if (type == PotionEffectType.REGENERATION) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, effect.getDuration(), effect.getAmplifier()));
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if ("undead".equals(playerClasses.get(player.getUniqueId()))) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onArmorChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if ("undead".equals(playerClasses.get(player.getUniqueId()))) {
            int armorWeight = getArmorWeight(player);
            double speedModifier = 0.1 - (armorWeight * 0.01);
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedModifier);
        }
    }

    private int getArmorWeight(Player player) {
        int weight = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                weight++;
            }
        }
        return weight;
    }

    public void giveFishingRod(Player player) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.addEnchant(Enchantment.LURE, 3, true);
        meta.setUnbreakable(true);
        meta.displayName(Component.text("§bУдочка Рыбака"));
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        rod.setItemMeta(meta);
        player.getInventory().setItem(0, rod);  
    }

    public Map<UUID, String> getPlayerClasses() {
        return playerClasses;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String pClass = playerClasses.get(player.getUniqueId());

        if (pClass != null && pClass.equals("kiborg")) {
            if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                    && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                
                long lastTime = kiborgCooldown.getOrDefault(player.getUniqueId(), 0L);
                long now = System.currentTimeMillis();

                if (now - lastTime < 60000) {
                    long timeLeft = (60000 - (now - lastTime)) / 1000;
                    player.sendMessage("§cВы пока не можете использовать способность, осталось " + timeLeft + " сек.");
                    return;
                }

                kiborgCooldown.put(player.getUniqueId(), now);
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();

                Fireball fireball = player.getWorld().spawn(eye.add(dir.multiply(1)), Fireball.class);
                fireball.setShooter(player);
                fireball.setVelocity(dir.multiply(1.5));
                fireball.setIsIncendiary(false);
                fireball.setYield(2.0f);
                // Стрельба фаерболом
            }
        }
    }

    private boolean isPlantBased(ItemStack item) {
        Material type = item.getType();
        return type == Material.APPLE || type == Material.CARROT || type == Material.POTATO || type == Material.BREAD;
    }

    private boolean isFish(ItemStack item) {
        Material type = item.getType();
        return type == Material.COD || type == Material.SALMON || type == Material.TROPICAL_FISH;
    }


    @EventHandler
    public void onEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String pClass = playerClasses.get(player.getUniqueId());
        if (pClass == null) return;

        Material type = event.getItem().getType();
        
        if ("vegetarian".equals(pClass)) {
            if (isMeat(type)) {
                // Вегетарианец получает отрицательный эффект от мяса
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1, false, false));
                player.sendMessage("§cВегетарианец плохо переваривает мясо!");
            } else {
                // Позитивные эффекты от правильной еды
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
                player.sendMessage("§aЗдоровая пища приносит пользу!");
            }
        } else if ("automaton".equals(pClass)) {
            // Автоматон может есть любую еду, но получает в 2 раза меньше насыщения
            event.setCancelled(true);
            
            int originalSaturation = getFoodSaturation(type);
            int newSaturation = originalSaturation / 2;

            // Удаляем 1 предмет из стека (имитация поедания)
            ItemStack item = event.getItem();
            int amount = item.getAmount();
            if (amount > 1) {
                item.setAmount(amount - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // Обновляем уровень насыщенности (максимум = 20)
            int currentFood = player.getFoodLevel();
            int newFoodLevel = Math.min(currentFood + newSaturation, 20);
            player.setFoodLevel(newFoodLevel);

            long now = System.currentTimeMillis();
            long lastMessageTime = lastEatMessage.getOrDefault(player.getUniqueId(), 0L);

            if (now - lastMessageTime > 1000) { // 1000 = 3 сек
                player.sendMessage("§eВы съели " + type + ", восстановлено " + newSaturation + " единиц голода.");
                lastEatMessage.put(player.getUniqueId(), now);
            }
                
        }
    }

    public int getFoodSaturation(Material type) {
        switch (type) {
            case APPLE: return 2;
            case BAKED_POTATO: return 3;
            case BEEF: return 4;
            case BREAD: return 2;
            case CARROT: return 2;
            case COOKED_BEEF: return 4;
            case COOKED_CHICKEN: return 3;
            case COOKED_MUTTON: return 3;
            case COOKED_PORKCHOP: return 3;
            case COOKED_RABBIT: return 2;
            case COOKED_SALMON: return 3;
            case COOKIE: return 1;
            case GOLDEN_APPLE: return 4;
            case GOLDEN_CARROT: return 5 ;
            case MELON_SLICE: return 2;
            case MUSHROOM_STEW: return 5 ;
            case PUMPKIN_PIE: return 4 ;
            case RABBIT_STEW: return 3 ;
            case SWEET_BERRIES: return 1;
            case TROPICAL_FISH: return 1;
            default: return 2; // Если еды нет в списке, вернуть стандартное значение
        }
    }

    // Метод для проверки, является ли материал мясом
    private boolean isMeat(Material type) {
        return type == Material.BEEF || type == Material.COOKED_BEEF ||
            type == Material.CHICKEN || type == Material.COOKED_CHICKEN ||
            type == Material.PORKCHOP || type == Material.COOKED_PORKCHOP ||
            type == Material.MUTTON  || type == Material.COOKED_MUTTON  ||
            type == Material.RABBIT  || type == Material.COOKED_RABBIT;
    }

    // Метод для применения эффектов для класса "automaton"
    // Этот метод нужно вызывать, например, из обработчика PlayerMoveEvent
    private void applyAutomatonEffects(Player player) {
        // Получаем класс игрока из вашей карты (предполагается, что playerClasses у вас объявлена глобально)
        String pClass = playerClasses.get(player.getUniqueId());
        if (pClass == null) return;
        
        if (pClass.equals("automaton")) {
            // Применяем вечное замедление I (SLOWNESS, уровень 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
            
            // Если игрок (автоматон) стоит на "тяжёлом" блоке, применяем дополнительное замедление II
            Material groundType = player.getLocation().subtract(0, 1, 0).getBlock().getType();
            if (isRoughTerrain(groundType)) {
                player.sendMessage("§cВ ваши шестерёнки попали камни, и их заедает!");
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1)); // Замедление II на 5 сек
            }
        }
    }

    private void applyAutoHeal() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("automaton".equals(playerClasses.get(player.getUniqueId()))) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                    if (currentHealth < maxHealth) {
                        player.setHealth(Math.min(currentHealth + 1, maxHealth));
                    }
                }
            }
        }, 100L, 100L);
    }


    // 🔥 Обработчик урона (автоматон наносит +2 урона, обрабатываем перегрузку)
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            String pClass = playerClasses.get(attacker.getUniqueId());

            if ("automaton".equals(pClass)) {
                event.setDamage(event.getDamage() + 2);
                handleOverload(attacker);
            }
        }
    }

    // ⚙️ Перегрузка (3 удара за 2 сек → Сила I на 2 сек → Усталость 10 сек)
    private void handleOverload(Player attacker) {
        long now = System.currentTimeMillis();
        attackTimestamps.putIfAbsent(attacker.getUniqueId(), new ArrayList<>());
        List<Long> timestamps = attackTimestamps.get(attacker.getUniqueId());
        timestamps.add(now);
        timestamps.removeIf(time -> (now - time) > 2000);

        if (timestamps.size() >= 3) {
           attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
            Bukkit.getScheduler().runTaskLater(this, () -> {
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, false));
            }, 40L);
            timestamps.clear();
        }
    }

    // 🔩 Устойчивость к ядам (уменьшает длительность POISON и WITHER в 2 раза)
    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if ("automaton".equals(playerClasses.get(player.getUniqueId()))) {
                PotionEffect newEffect = event.getNewEffect();
                if (newEffect != null) {
                    PotionEffectType type = newEffect.getType();
                    if (type == PotionEffectType.POISON || type == PotionEffectType.WITHER) {
                        event.setCancelled(true);
                        player.addPotionEffect(new PotionEffect(type, newEffect.getDuration() / 2, newEffect.getAmplifier()));
                    }
                }
            }
        }
    }

    // 💥 Самоуничтожение (взрыв радиусом 2 при смерти)
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if ("automaton".equals(playerClasses.get(player.getUniqueId()))) {
                Location loc = player.getLocation();
                player.getWorld().createExplosion(loc, 2F, false, true);
            }
        }
    }


    // 📌 Проверка "тяжёлых" поверхностей для замедления
    private boolean isRoughTerrain(Material type) {
        return type == Material.GRAVEL || type == Material.SAND || type == Material.RED_SAND ||
               type == Material.SOUL_SAND || type == Material.SOUL_SOIL ||
               type == Material.COARSE_DIRT || type == Material.DIRT || type == Material.MYCELIUM;
    }

    // 🔹 Обновляем денежное состояние игрока
    private void updateWealth(Player player) {
        double wealth = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType() == Material.DIAMOND) {
                    wealth += item.getAmount() * 0.5; // 🔹 Алмаз +0.5
                } else if (item.getType() == Material.GOLD_INGOT) {
                    wealth += item.getAmount() * 0.25; // 🔹 Золото +0.25
                } else if (item.getType() == Material.DIAMOND_BLOCK) {
                    wealth += item.getAmount() * 4; // 🔹 Алмазный блок = 4 состояния
                } else if (item.getType() == Material.GOLD_BLOCK) {
                    wealth += item.getAmount() * 2.25; // 🔹 Золотой блок = 2.25 состояния
                }
            }
        }

        playerWealth.put(player.getUniqueId(), wealth);

        // 🔹 Применение эффектов в зависимости от состояния
        if (wealth < 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 0, false, false));
            player.setMaxHealth(12.0);
        } else if (wealth < 10) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, false, false));
            player.setMaxHealth(17.0);
        } else if (wealth < 20) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.HASTE);
            player.setMaxHealth(19.0);
        } else if (wealth < 30) {
            player.setMaxHealth(21.0);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.HASTE);
        } else if (wealth < 32) {
            player.setMaxHealth(24.0);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.HASTE);
        }
    }

    // 🔹 Автоматическая проверка состояния каждые 5 сек
    public void startWealthCheck() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("richman".equals(playerClasses.get(player.getUniqueId()))) {
                    updateWealth(player);
                }
            }
        }, 100L, 100L);
    }

    // 🔹 Бормотание богача
    public void startRichManMurmur() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("richman".equals(playerClasses.get(player.getUniqueId()))) {
                    double wealth = playerWealth.getOrDefault(player.getUniqueId(), 0.0);

                    if (wealth >= 5 && wealth < 10) {
                        if (new Random().nextInt(1) == 1) {
                        player.sendMessage("§7[Бормочет]: Где же мои драгоценности...");
                        } else {
                            player.sendMessage("§7[Бормочет]: Крипта, Арбетраж, Гараж ...");
                        }
                        
                    } else if (wealth >= 10 && wealth < 20) {
                        player.sendMessage("§7[Бормочет]: Мне бы ещё немного золота...");
                    } else if (wealth >= 20 && wealth < 30) {
                        player.sendMessage("§6[Бормочет]:  1 монета, 2 монета, 3 монета, 4...");
                    } else if (wealth >= 32) {
                        player.sendMessage("§c[Бормочет]: Я владыка богатства! Злато — моя душа...");
                    }
                }
            }
        }, 600L, 1200L);
    }
        public void startWealthShadowsEffect() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("richman".equals(playerClasses.get(player.getUniqueId()))) {
                    double wealth = playerWealth.getOrDefault(player.getUniqueId(), 0.0);
                    
                    if (wealth > 15) {
                        playWhisperSounds(player);
                        
                    }

                    if (wealth > 20  && Math.random() < 0.2) { // 🔹 20% шанс появления тени
                        spawnShadowEntity(player);
                    }
                }
            }
        }, 6000L, 9000L); // 🔹 Каждые 5-7.5 минут
    }

    // 🔹 Проигрывание странных звуков
    private void playWhisperSounds(Player player) {
        Sound[] eerieSounds = { Sound.ENTITY_WITHER_SPAWN.ENTITY_ENDERMAN_AMBIENT, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT };
        Sound randomSound = eerieSounds[new Random().nextInt(eerieSounds.length)];

        player.getWorld().playSound(player.getLocation(), randomSound, 0.7f, 1.2f);
        player.sendMessage("§8Вы слышите странные шепоты... Кажется, кто-то следит за вами.");

    }

    // 🔹 Иллюзорная тень (моб, который исчезает)
    private void spawnShadowEntity(Player player) {
        Location loc = player.getLocation().add(
            (Math.random() * 6 - 3), 0, (Math.random() * 6 - 3) // 🔹 Смещение вокруг игрока
        );

        Entity shadow = player.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
        shadow.setSilent(true);
        
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (shadow.isValid()) shadow.remove(); // 🔹 Тень исчезает через 10 сек
        }, 200L);

        player.sendMessage("§8Вы чувствуете чей-то взгляд... но никого нет.");
    }

  // 🔹 Создание иллюзорной алмазной руды
    public void startIllusoryOreSpawn() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("richman".equals(playerClasses.get(player.getUniqueId()))) {
                    double wealth = playerWealth.getOrDefault(player.getUniqueId(), 0.0);
                    if (wealth > 32 && Math.random() < 0.5) {
                        Location loc = player.getLocation().clone().add(
                            (Math.random() * 10 - 5), // 🔹 Смещение X
                            (Math.random() * 5 - 2), // 🔹 Смещение Y
                            (Math.random() * 10 - 5)  // 🔹 Смещение Z
                        );

                        spawnIllusoryOre(player, loc, 30); // 30 сек исчезновения
                    }
                }
            }
        }, 12000L, 18000L); // 🔹 Раз в 10-15 минут
    }
    @EventHandler
    public void onVillagerTradeDynamicPrice(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playerClasses.containsKey(uuid)) return;
        if (!playerClasses.get(uuid).equals("Richman")) return;

        double wealth = playerWealth.getOrDefault(uuid, 0.0);
        int priceBoost = (int) (wealth / 2); // например, 1 за каждые 1000       

        Bukkit.getScheduler().runTaskLater(this, () -> {
            List<MerchantRecipe> boostedRecipes = new ArrayList<>();

            for (MerchantRecipe original : villager.getRecipes()) {
                MerchantRecipe modified = new MerchantRecipe(original.getResult(), original.getMaxUses());
                List<ItemStack> boostedIngredients = new ArrayList<>();

                for (ItemStack ingredient : original.getIngredients()) {
                    ItemStack boosted = ingredient.clone();
                    boosted.setAmount(ingredient.getAmount() + priceBoost); // Увеличиваем цену
                    boostedIngredients.add(boosted);
                }

                modified.setIngredients(boostedIngredients);
                boostedRecipes.add(modified);
            }

            villager.setRecipes(boostedRecipes);
            player.sendMessage("§6Ваше богатство привлекло внимание... Цены взлетели.");
        }, 2L); // Подождать, пока откроется GUI
    }


    private void spawnIllusoryOre(Player player, Location loc, int duration) {
        Block block = loc.getBlock();
        block.setType(Material.DIAMOND_ORE);
        illusoryOres.put(loc, player.getUniqueId()); // 🔹 Запоминаем руду

        if (duration != Integer.MAX_VALUE) { // 🔹 Если это не "вечная руда"
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (illusoryOres.containsKey(loc)) {
                    block.setType(Material.AIR);
                    illusoryOres.remove(loc);
                }
            }, duration * 20L); // 🔹 Перевод секунд в тики
        }
    }

    @EventHandler
    public void onPistonMove(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (illusoryOres.containsKey(block.getLocation())) {
                Location newLoc = block.getLocation().add(event.getDirection().getDirection());
                illusoryOres.put(newLoc, illusoryOres.get(block.getLocation())); // 🔹 Обновляем координаты
                illusoryOres.remove(block.getLocation());
            }
        }
    }

   @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        Player player = event.getPlayer();

        if (illusoryOres.containsKey(loc) && illusoryOres.get(loc).equals(player.getUniqueId())) {
            event.setDropItems(false); // 🔹 Убираем дроп
            block.setType(Material.AIR); // 🔹 Удаляем блок
            illusoryOres.remove(loc);
            player.sendMessage("§7Неет! Не исчезай! Нееет!");
        }
    }

    public void startGreedMadnessEffect() {
    Bukkit.getScheduler().runTaskTimer(this, () -> {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if ("richman".equals(playerClasses.get(player.getUniqueId()))) {
                double greed = playerWealth.getOrDefault(player.getUniqueId(), 0.0);
                
                if (greed > 30) {
                    applyMadnessEffect(player);
                }
            }
        }
    }, 12000L, 18000L); // 🔹 Раз в 10-15 минут
}

private void applyMadnessEffect(Player player) {
    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false)); // 🔹 Тьма на 5 сек
    player.setMaxHealth(10.0); // 🔹 Уменьшаем ХП до 10

    ItemStack[] inventory = player.getInventory().getContents();
    List<ItemStack> validItems = new ArrayList<>();

    // 🔹 Собираем все предметы, которые можно выбросить
    for (ItemStack item : inventory) {
        if (item != null && item.getType() != Material.AIR) {
            validItems.add(item);
        }
    }

    if (!validItems.isEmpty()) {
        // 🔹 Выбираем случайный предмет
        ItemStack randomItem = validItems.get(new Random().nextInt(validItems.size()));

        // 🔹 Убираем предмет из инвентаря
        player.getInventory().removeItem(randomItem);

        // 🔹 Выбрасываем предмет в мир
        player.getWorld().dropItemNaturally(player.getLocation(), randomItem);
        
        player.sendMessage("§cВы уронили " + randomItem.getType().name().toLowerCase().replace("_", " ") + "!");
    } // 🔹 Выбрасываем случайный предмет
    
    Bukkit.getScheduler().runTaskLater(this, () -> {
        player.setMaxHealth(21.0); // 🔹 Возвращаем нормальное ХП после 5 секунд
    }, 100L);
    
    player.sendMessage("§8§lВаше богатство затуманило рассудок... Вы чувствуете слабость.");
}


}