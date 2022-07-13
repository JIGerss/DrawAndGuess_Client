package Client;

import org.apache.commons.codec.digest.DigestUtils;
import com.alibaba.fastjson.JSON;
import processing.awt.PSurfaceAWT;
import processing.core.PApplet;
import processing.core.PFont;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Lobby extends PApplet {
    public static boolean isGaming = false;
    private final int WIDTH = 900, HEIGHT = 550;
    private final Room[] rooms = new Room[6];
    private String URL = "\u001C\u0000\u0000\u0004N[[EDEZG@ZGLZEGGNLDMD[";
    private boolean isVisible = true;
    private boolean hasStartAGame = false;
    private int gameNum = 0;
    private int requestTime = 60;
    private String[] users;
    private Button logout;
    private User Player;
    private Game[] games;

    public static void main(String[] args) {
        PApplet.main("Client.Lobby");
    }

    private String encryptToMD5(String str) {
        return DigestUtils.md5Hex(str);
    }

    public void setup() {
        size(WIDTH, HEIGHT);
        URL = convertMD5(URL);
        boolean isSucceed = false;
        while (!isSucceed) {
            //Enter account
            String name = JOptionPane.showInputDialog(null, "输入用户名：", "你画我猜", JOptionPane.INFORMATION_MESSAGE);
            if (name == null || name.equals("")) exitLobby();
            String RegTest = HttpRequest.sendGet(URL + "users/hasreg/" + name, "");

            boolean isCorrect = false;
            while (!isCorrect) {
                //Enter password
                String psw;
                if (RegTest.equals("N")) {
                    psw = JOptionPane.showInputDialog(null, "请设置新用户" + name + "的密码：", "你画我猜 -注册", JOptionPane.INFORMATION_MESSAGE);
                    if (psw == null) break;
                    if (psw.equals("")) continue;
                    HttpRequest.doPost(URL + "users/reg/" + name + "/" + encryptToMD5(psw), "", "");
                    String login = HttpRequest.doPost(URL + "users/login/" + name + "/" + encryptToMD5(psw), "", "");
                    Player = JSON.parseObject(login, User.class);
                    isCorrect = true;
                    isSucceed = true;
                } else {
                    psw = JOptionPane.showInputDialog(null, "请输入用户" + name + "的密码：", "你画我猜 -登录", JOptionPane.INFORMATION_MESSAGE);
                    if (psw == null) break;
                    if (psw.equals("")) continue;
                    String loginTest = HttpRequest.doPost(URL + "users/login/" + name + "/" + encryptToMD5(psw), "", "");
                    if (loginTest != null) {
                        if (loginTest.contains("password is incorrect")) {
                            JOptionPane.showMessageDialog(null, "密码错误！", "你画我猜 -密码错误", JOptionPane.WARNING_MESSAGE);
                        } else if (loginTest.contains("Already Exists")) {
                            JOptionPane.showMessageDialog(null, "该账号已登录！", "你画我猜 -账号已登录", JOptionPane.WARNING_MESSAGE);
                            break;
                        } else {
                            System.out.println(loginTest);
                            Player = JSON.parseObject(loginTest, User.class);
                            isCorrect = true;
                            isSucceed = true;
                        }
                    } else
                        error();
                }
            }
        }
        setVariables();
        logout = new Button(700, 430, 150, 60);
        System.out.println("Succeed to login!Hello " + Player.getUserName() + " " + Player.getUserId());
        PFont myFont = createFont("SIMHEI", 30);
        textFont(myFont);
        noStroke();
        strokeWeight(2);
        frameRate = 60;
    }

    public void draw() {
        if (!isVisible && !isGaming) {
            Frame frame = ((PSurfaceAWT.SmoothCanvas) ((PSurfaceAWT) surface).getNative()).getFrame();
            frame.setVisible(true);
            isVisible = true;
            setVariables();
        } else if (isGaming && !isVisible)
            return;
        if (requestTime == 60) {
            requestTime = 0;
            setVariables();
        } else
            requestTime++;
        background(253, 248, 229);
        textSize(30);
        fill(130, 130, 130);
        text("在线用户(" + users.length + ")：", (float) (WIDTH / 1.3), (float) (HEIGHT / 10));
        text("点击房间进入游戏：", 30, 50);
        textSize(25);
        for (int i = 0; i < users.length; i++) {
            if (i >= 7) {
                text("···", (float) (WIDTH / 1.24), (float) (HEIGHT / 9 + 30 + 30 * i));
                break;
            }
            text(users[i], (float) (WIDTH / 1.24), (float) (HEIGHT / 9 + 30 + 30 * i));
        }
        for (Room room : rooms) {
            if (mouseX > room.button.x && mouseX < room.button.x + room.button.width && mouseY > room.button.y && mouseY < room.button.y + room.button.height)
                fill(190, 163, 162);
            else {
                if (!room.isGame)
                    fill(230, 198, 26);
                else
                    fill(255, 198, 180);
            }
            rect(room.button.x, room.button.y, room.button.width, room.button.height, 50);
            fill(130, 130, 130);
            text(room.numberOfPlayer, room.button.x + 45, room.button.y + 56);
        }
        if (mouseX > logout.x && mouseX < logout.x + logout.width && mouseY > logout.y && mouseY < logout.y + logout.height)
            fill(190, 163, 162);
        else
            fill(170, 163, 162);
        rect(logout.x, logout.y, logout.width, logout.height, 2);
        fill(253, 248, 229);
        text("登出游戏", logout.x + 27, logout.y + 40);

    }

    public void mousePressed() {
        setVariables();
        if (mouseX > logout.x && mouseX < logout.x + logout.width && mouseY > logout.y && mouseY < logout.y + logout.height) {
            logout();
            exitLobby();
        }
        for (Room room : rooms) {
            if (mouseX > room.button.x && mouseX < room.button.x + room.button.width && mouseY > room.button.y && mouseY < room.button.y + room.button.height) {
                if (!room.isGame) {
                    Game game = createGame();
                    System.out.println(JSON.toJSONString(game));
                    if (game != null) {
                        System.out.println("Set Answer to " + game.getAnswer());
                        joinGame(game, Player, true);
                    }
                } else {
                    joinGame(room.game, Player, false);
                }
            }
        }

    }

    private void setVariables() {
        users = getUsers();
        games = getGames();
        gameNum = games.length;
        setRooms();
    }

    private Game createGame() {
        try {
            String answer = JOptionPane.showInputDialog(null, "请设置房间题目：", "你画我猜", JOptionPane.INFORMATION_MESSAGE);
            if (answer == null) return null;
            String jsonStr = JSON.toJSONString(Player);
            String result = HttpRequest.doPost(URL + "games/create/" + answer, "", jsonStr);
            return JSON.parseObject(result, Game.class);
        }catch (com.alibaba.fastjson.JSONException e){
            error();
        }
        return null;
    }

    private void joinGame(Game game, User Player, boolean isHost) {
        isGaming = true;
        isVisible = false;
        Frame frame = ((PSurfaceAWT.SmoothCanvas) ((PSurfaceAWT) surface).getNative()).getFrame();
        frame.setVisible(false);
        Clients.isDrawer = isHost;
        Clients.gameId = game.getId();
        Clients.Player = Player;
        if (!hasStartAGame) {
            hasStartAGame = true;
            Clients.main(args);
        }
    }

    private void setRooms() {
        for (int i = 0; i < rooms.length; i++) {
            if (gameNum > i) {
                rooms[i] = new Room(new Button(150 + (i % 2) * 250, 85 + (i / 2) * 150, 100, 100), games[i]);
                String json = HttpRequest.sendGet(URL + "games/" + games[i].getId() + "/players", "");
                List<String> list = JSON.parseArray(json, String.class);
                rooms[i].numberOfPlayer = list.size();
                rooms[i].isGame = true;
            } else {
                rooms[i] = new Room(new Button(150 + (i % 2) * 250, 85 + (i / 2) * 150, 100, 100));
            }
        }
    }

    private void logout() {
        String userJson = JSON.toJSONString(Player);
        String result = HttpRequest.doDelete(URL + "users/logout/" + Player.getUserName() + "/" + Player.getUserId(), "", userJson);
        System.out.println("Succeed to logout!" + result);
    }

    private String[] getUsers() {
        String json = HttpRequest.sendGet(URL + "users/list", "");
        List<String> list = JSON.parseArray(json, String.class);
        String[] USERS = new String[list.size()];
        int cur = 0;
        for (String s : list)
            USERS[cur++] = s;
        return USERS;
    }

    private Game[] getGames() {
        String json = HttpRequest.sendGet(URL + "games", "");
        List<String> list = JSON.parseArray(json, String.class);
        int size = 0;
        for (String s : list)
            if (!JSON.parseObject(s, Game.class).isEnd()) size++;
        Game[] GAMES = new Game[size];
        int cur = 0;
        for (String s : list) {
            Game temp = JSON.parseObject(s, Game.class);
            if (!temp.isEnd()) GAMES[cur++] = temp;
        }
        return GAMES;
    }

    private String convertMD5(String inStr) {
        char[] a = inStr.toCharArray();
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (a[i] ^ 't');
        }
        return new String(a);
    }

    private void exitLobby() {
        try {
            System.exit(0);
        } catch (SecurityException securityException) {
            System.out.println("Error!!");
        }
    }

    private void error() {
        JOptionPane.showMessageDialog(null, "错误！！", "你画我猜", JOptionPane.WARNING_MESSAGE);
        exitLobby();
    }

    public void settings() {
        size(WIDTH, HEIGHT);
    }

}
