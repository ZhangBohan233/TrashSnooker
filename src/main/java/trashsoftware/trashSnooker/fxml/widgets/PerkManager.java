package trashsoftware.trashSnooker.fxml.widgets;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.fxml.CareerView;
import trashsoftware.trashSnooker.util.DataLoader;

public class PerkManager {
    
    public static final int AIMING = 1;  // 从1开始，连续；如果要改就要改很多地方
    public static final int CUE_PRECISION = 2;
    public static final int POWER = 3;
    public static final int POWER_CONTROL = 4;
    public static final int SPIN = 5;
    public static final int SPIN_CONTROL = 6;
    public static final int ANTI_HAND = 7;
    public static final int REST = 8;

    private CareerView parent;
    private PlayerPerson.ReadableAbility ability;
    private PlayerPerson.ReadableAbility previewAbility;
    private int availPerks;
    private int[] addedPerks = new int[8];

    public PerkManager(CareerView parent, int availPerks, PlayerPerson.ReadableAbility ability) {
        this.availPerks = availPerks;
        this.parent = parent;
        
        setAbility(ability);
    }

    private void setAbility(PlayerPerson.ReadableAbility ability) {
        this.ability = ability;
        this.previewAbility = ability.clone();
    }
    
    public void synchronizePerks() {
        clearSelections();
        availPerks = CareerManager.getInstance().getHumanPlayerCareer().getAvailablePerks();
    }
    
    public PlayerPerson.ReadableAbility getOriginalAbility() {
        return ability;
    }
    
    public PlayerPerson.ReadableAbility getShownAbility() {
        return previewAbility;
    }

    public int getAvailPerks() {
        return availPerks;
    }
    
    public int addPerkTo(int cat) {
        availPerks--;
        addedPerks[cat - 1] += 1;
        previewAbility.addPerks(cat, 1);
        parent.notifyPerksChanged();
        return addedPerks[cat - 1];
    }
    
    public int getPerksAddedTo(int cat) {
        return addedPerks[cat - 1];
    }
    
    public void clearSelections() {
        this.previewAbility = this.ability.clone();
        
        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                addedPerks[i] = 0;
            }
        }
        availPerks += sum;
    }
    
    public int applyPerks() {
        this.ability = previewAbility;
        DataLoader.getInstance().updatePlayer(this.ability.toPlayerPerson());
        this.previewAbility = this.ability.clone();
        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                addedPerks[i] = 0;
            }
        }
        return sum;
    }
}
