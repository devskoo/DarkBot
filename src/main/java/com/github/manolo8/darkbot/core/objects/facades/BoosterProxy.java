package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.ObjArray;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import com.github.manolo8.darkbot.gui.utils.CachedFormatter;
import com.github.manolo8.darkbot.utils.Time;
import eu.darkbot.api.managers.BoosterAPI;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.manolo8.darkbot.Main.API;
import static com.github.manolo8.darkbot.Main.UPDATE_LOCKER;

public class BoosterProxy extends Updatable implements BoosterAPI {
    public List<Booster> boosters = new ArrayList<>();

    private final ObjArray boostersArr = ObjArray.ofVector(true);

    @Override
    public void update() {
        long data = API.readMemoryLong(address + 48) & ByteUtils.ATOM_MASK;

        boostersArr.update(API.readMemoryLong(data + 0x48));
        synchronized (UPDATE_LOCKER) {
            boostersArr.sync(boosters, Booster::new);
        }
    }

    public static class Booster extends Auto implements BoosterAPI.Booster {
        public double amount, cd;
        public String category, name;
        public BoosterCategory cat;

        private final CachedFormatter simpleStringFmt = CachedFormatter.ofPattern("%3s %2.0f%% %s");

        private final ObjArray subBoostersArr = ObjArray.ofVector(true);
        private final ObjArray attributesArr = ObjArray.ofVector(true);

        @Override
        public void update() {
            String oldCat = this.category;
            this.category = API.readMemoryString(address, 0x20);
            if (!Objects.equals(this.category, oldCat))
                this.cat = BoosterCategory.of(category);
            this.name     = API.readMemoryString(address, 0x40); //0x48 description;
            this.amount   = API.readMemoryDouble(address + 0x50);
            this.subBoostersArr.update(API.readMemoryLong(address + 0x30));

            double min = Double.POSITIVE_INFINITY, curr;
            for (int i = 0; i < subBoostersArr.getSize(); i++)
                if ((curr = API.readMemoryDouble(subBoostersArr.get(i), 0x30, 0x38)) > 0 && curr < min) min = curr;
            this.cd = min;

            if (cd != Double.POSITIVE_INFINITY && amount <= 0) {
                double amount = 0;

                for (int i = 0; i < subBoostersArr.getSize(); i++) {
                    attributesArr.update(API.readLong(subBoostersArr.getPtr(i), 0x28, 0x40, 0x20, 0x88));

                    for (int j = 0; j < attributesArr.getSize(); j++) {
                        long attribute = attributesArr.getPtr(j);

                        if (API.readLong(attribute + 0x20) == API.readLong(address + 0x20)) {
                            amount += API.readDouble(attribute + 0x28);
                            break;
                        }
                    }
                }

                this.amount = amount;
            }
        }

        public String toSimpleString() {
            return simpleStringFmt.format(Time.secondsToShort(cd), amount, cat.getSmall(category));
        }

        public Color getColor() {
            return cat.getColor();
        }

        @Override
        public double getAmount() {
            return amount;
        }

        @Override
        public double getRemainingTime() {
            return cd;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private enum BoosterCategory {
        ABILITY_COOLDOWN_TIME   ("CD"    , new Color(0xFFC000)),
        DAMAGE                  ("DMG"   , new Color(0xFD0400)),
        DAMAGE_PVP              ("DMG PVP", new Color(0xBD0805)),
        DAMAGE_FACTIONS         ("DMG FAC", new Color(0x941311)),
        DAMAGE_NPC              ("DMG NPC", new Color(0x7E0807)),
        EXPERIENCE_POINTS       ("EXP"   , new Color(0xF77800)),
        HITPOINTS               ("HP"    , new Color(0x049104)),
        HONOUR_POINTS           ("HON"   , new Color(0xFF8080)),
        REPAIR                  ("REP"   , new Color(0xA93DE4)),
        COLLECT_RESOURCES       ("RES"   , new Color(0xEAD215)),
        SHIELD                  ("SHD"   , new Color(0x69EBFF)),
        SHIELD_REGENERATION     ("SHDR"  , new Color(0x3B64BD)),
        AMOUNT                  ("AMT"   , new Color(0xFFCC00)),
        COLLECT_RESOURCES_NEWBIE("DBL"   , new Color(0xFFF3CF)),
        CHANCE                  ("CHN"   , new Color(0xFFD100)),
        EVENT_AMOUNT            ("EVT AM", new Color(0x05B6E3)),
        EVENT_CHANCE            ("EVT CH", new Color(0x00C6EE)),
        SPECIAL_AMOUNT          ("SP AM" , new Color(0xFFFFFF)),
        PET_EXP_BOOSTER         ("PET XP", new Color(0xF77800)),
        LASER_HIT_CHANCE        ("LAS HC", new Color(0x0F6164)),
        ROCKET_HIT_CHANCE       ("ROC HC", new Color(0x0B7E81)),
        UNKNOWN                 ("?"     , new Color(0x808080)) {
            @Override
            public String getSmall(String category) {
                if (category.isEmpty()) return "";
                return Arrays.stream(category.split("_"))
                        .map(str -> str.length() <= 3 ? str : str.substring(0, 3))
                        .collect(Collectors.joining(" "));
            }
        };

        private final String small;
        private final Color color;

        BoosterCategory(String small, Color color) {
            this.small = small;
            this.color = color;
        }

        public String getSmall(String category) {
            return this.small;
        }
        public Color getColor() {
            return this.color;
        }

        public static BoosterCategory of(String category) {
            for (BoosterCategory cat : BoosterCategory.values()) {
                if (cat.name().equalsIgnoreCase(category)) return cat;
            }
            return UNKNOWN;
        }

    }

    @Override
    public List<Booster> getBoosters() {
        return boosters;
    }
}
