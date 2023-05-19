package haven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AccountList extends Widget {
    public static final String SAVED_TOKENS = "savedTokens";
    public static final LinkedHashMap<String, String> accountmap = new LinkedHashMap<>();
    private static final Coord SZ = UI.scale(240, 30);

    static {
        AccountList.loadAccounts();
    }

    public int height, y;
    public final List<Account> accounts = new ArrayList<>();

    static void loadAccounts() {
        File accountList = new File(SAVED_TOKENS);
         if(accountList.exists()) {
             try {
                 for(String s : Files.readAllLines(Paths.get(accountList.toURI()), StandardCharsets.UTF_8)) {
                     String[] split = s.split("(;)");
                     if(!accountmap.containsKey(split[0]))
                         accountmap.put(split[0], split[1]);
                 }
             } catch(IOException e) {
                 e.printStackTrace();
             }
         }
    }

    public static void storeAccount(String name, String token) {
        synchronized(accountmap) {
            accountmap.put(name, token);
        }
        saveAccounts();
    }

    public static void removeAccount(String name) {
        synchronized(accountmap) {
            accountmap.remove(name);
        }
        saveAccounts();
    }

    public static void saveAccounts() {
        synchronized(accountmap) {
            try {
                BufferedWriter bw = Files.newBufferedWriter(Paths.get(new File(SAVED_TOKENS).toURI()), StandardCharsets.UTF_8);
                for(Map.Entry<String, String> e : accountmap.entrySet()) {
                    bw.write(e.getKey() + ";" + e.getValue() + "\n");
                }
                bw.flush();
                bw.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO: find how to incorporate hostname without breaking stuff
    public static byte[] getToken(String user, String hostname) {
        synchronized (accountmap) {
            String token = accountmap.get(user);
            if(token == null){
                return null;
            } else {
                return Utils.hex2byte(token);
            }
        }
    }

    public static void setToken(String user, String hostname, byte[] token) {
        if(token == null) {
            removeToken(user, hostname);
        } else {
            storeAccount(user, Utils.byte2hex(token));
        }
    }

    public static void removeToken(String user, String hostname) {
        removeAccount(user);
    }

    public static class Account {
        public String name, token;
        Button plb, del;

        public Account(String name, String token) {
            this.name = name;
            this.token = token;
        }
    }

    public AccountList(int height) {
        super();
        this.height = height;
        this.sz = new Coord(SZ.x, SZ.y * height);
        y = 0;

        for (Map.Entry<String, String> entry : accountmap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void scroll(int amount) {
        y += amount;
        synchronized(accounts) {
            if(y > accounts.size() - height)
                y = accounts.size() - height;
        }
        if(y < 0)
            y = 0;
    }

    public void draw(GOut g) {
        Coord step = UI.scale(1, 6);
        Coord cc = UI.scale(10, 10);
        synchronized (accounts) {
            for (Account account : accounts) {
                account.plb.hide();
                account.del.hide();
            }
            for (int i = 0; (i < height) && (i + this.y < accounts.size()); i++) {
                Account account = accounts.get(i + this.y);
                account.plb.show();
                account.plb.c = cc;
                account.del.show();
                account.del.c = cc.add(account.plb.sz.x + step.x, step.y);
                cc = cc.add(0, SZ.y);
            }
        }
        super.draw(g);
    }

    public boolean mousewheel(Coord c, int amount) {
        scroll(amount);
        return (true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender instanceof Button) {
            synchronized(accounts) {
                for(Account account : accounts) {
                    if(sender == account.plb) {
                        super.wdgmsg("account", account.name, account.token);
                        break;
                    } else if(sender == account.del) {
                        remove(account);
                        break;
                    }
                }
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void add(String name, String token) {
        Account c = new Account(name, token);
        c.plb = add(new Button(UI.scale(160), name) {
            @Override
            protected boolean i10n() { return false; }
        });
        c.plb.hide();
        c.del = add(new Button(UI.scale(24), "X") {
            @Override
            protected boolean i10n() { return false; }
        });
        c.del.hide();
        synchronized (accounts) {
            accounts.add(c);
        }
    }

    public void remove(Account account) {
        synchronized(accounts) {
            accounts.remove(account);
        }
        scroll(0);
        removeAccount(account.name);
        ui.destroy(account.plb);
        ui.destroy(account.del);
    }
    public Account getAccountFromName(String name){
        synchronized (accounts) {
            for (Account account : accounts) {
                if (name.equals(account.name)){
                    return account;
                }
            }
        }
        return null;
    }
}