package cn.epicmc.client.hud;

public enum GameModeType {
    ACTION("行动模式"),
    CONTEST("夺点模式")
    ;
    private final String string;//预留游戏模式
    GameModeType(String string){
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
