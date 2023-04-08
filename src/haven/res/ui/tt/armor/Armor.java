package haven.res.ui.tt.armor;/* Preprocessed source code */
import haven.*;
import java.awt.image.BufferedImage;

/* >tt: Armor */
@haven.FromResource(name = "ui/tt/armor", version = 4) // ND: Doesn't matter, I'm overwriting it in Resource.java
public class Armor extends ItemInfo.Tip {
    public final int hard, soft;

    public Armor(Owner owner, int hard, int soft) {
        super(owner);
        this.hard = hard;
        this.soft = soft;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        return(new Armor(owner, (Integer)args[1], (Integer)args[2]));
    }

    public static class Fac implements ItemInfo.InfoFactory {
        public Fac() {}
        public ItemInfo build(Owner owner, Raw raw, Object... args) {
            return mkinfo(owner, args);
        }
    }

    public BufferedImage tipimg() {
        return(Text.render(String.format("Armor: %,d/%,d" +" ("+(hard+soft)+")", hard, soft)).img);
    }
}
