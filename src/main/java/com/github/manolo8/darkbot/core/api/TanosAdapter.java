package com.github.manolo8.darkbot.core.api;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.MapManager;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import com.github.manolo8.darkbot.utils.StartupParams;
import eu.darkbot.api.DarkBoat;
import eu.darkbot.api.DarkTanos;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.managers.OreAPI;

import java.util.function.BooleanSupplier;

import static com.github.manolo8.darkbot.Main.API;

public class TanosAdapter extends GameAPIImpl<DarkTanos,
        DarkTanos,
        DarkTanos,
        ByteUtils.StringReader,
        DarkTanos,
        TanosAdapter.DirectInteractionManager> {

    private final MapManager mapManager;

    public TanosAdapter(StartupParams params, DirectInteractionManager di, DarkTanos tanos, MapManager mapManager) {
        super(params, tanos, tanos, tanos, new ByteUtils.StringReader(tanos), tanos, di,
                GameAPI.Capability.LOGIN,
                GameAPI.Capability.INITIALLY_SHOWN,
                GameAPI.Capability.CREATE_WINDOW_THREAD,

                GameAPI.Capability.DIRECT_ENTITY_LOCK,
                GameAPI.Capability.DIRECT_MOVE_SHIP,
                GameAPI.Capability.DIRECT_COLLECT_BOX,
                GameAPI.Capability.DIRECT_REFINE,
                GameAPI.Capability.DIRECT_CALL_METHOD);

        this.mapManager = mapManager;
    }

    @Override
    public void rawKeyboardClick(char keyCode) {
        API.callMethod(3, API.readLong(mapManager.eventAddress, 0, 0x68), keyCode);
    }

    public static class DirectInteractionManager implements GameAPI.DirectInteraction {

        private final Main main;
        private final DarkTanos tanos;

        public DirectInteractionManager(Main main, DarkTanos tanos) {
            this.main = main;
            this.tanos = tanos;
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public void setMaxFps(int maxFps) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockEntity(int id) {
            tanos.lockEntity(id);
        }

        @Override
        public void moveShip(Locatable dest) {
            callMethod(10, tanos.readLong(main.mapManager.eventAddress), (long) dest.getX(), (long) dest.getY());
        }

        @Override
        public void collectBox(Locatable dest, long addr) {
            callMethod(10, tanos.readLong(main.mapManager.eventAddress), (long) dest.getX(), (long) dest.getY(), addr, 0);
        }

        @Override
        public void refine(long refineUtilAddress, OreAPI.Ore oreType, int amount) {
            tanos.refine(refineUtilAddress, oreType.getId(), amount);
        }

        @Override
        public long callMethod(int index, long... arguments) {
            long[] args = new long[arguments.length - 1];
            System.arraycopy(arguments, 1, args, 0, args.length);
            return tanos.callMethod(arguments[0], index, args);
        }
    }


}
