/* Preprocessed source code */
import haven.*;
import java.awt.Color;

/* >tt: OnBelt */
@haven.FromResource(name = "ui/tt/onbelt", version = 4)
public class OnBelt implements ItemInfo.InfoFactory {
    public static final Color olcol = new Color(255, 200, 0, 128);

    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
	class Info extends ItemInfo implements GItem.ColorInfo {
	    public Info(Owner owner) {super(owner);}
	    public Color olcol() {
		return(olcol);
	    }
	}
	return(new Info(owner));
    }
}
