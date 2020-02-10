package example.classes.mmorpg;

public class Character {
    public int stamina;

    public Character() {
        this.stamina = 100;
    }

    public void runTraining(WorldMap worldMap) {
        for(int i = 0; i < 10; i++) {
            worldMap.takeAStep(this);
        }
    }
}
