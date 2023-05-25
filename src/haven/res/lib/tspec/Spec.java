package haven.res.lib.tspec;

import haven.*;
import haven.GSprite.Owner;
import haven.ItemInfo.SpriteOwner;

import java.util.List;
import java.util.Random;

@FromResource(name = "ui/tspec", version = 5, override = true)
public class Spec implements Owner, SpriteOwner {
    private static final Object[] definfo = {
            new Object[]{Loading.waitfor(Resource.remote().load("ui/tt/defn"))},
    };
    public final Object[] info;
    public final ResData res;
    public final OwnerContext ctx;

    public Spec(ResData res, OwnerContext ctx, Object[] info) {
        this.res = res;
        this.ctx = ctx;
        this.info = (info == null) ? definfo : info;
    }

    public static final ClassResolver<UI> uictx = new ClassResolver<UI>()
            .add(Glob.class, ui -> ui.sess.glob)
            .add(Session.class, ui -> ui.sess);

    public static OwnerContext uictx(UI ui) {
        return (new OwnerContext() {
            public <C> C context(Class<C> cl) {
                return (uictx.context(cl, ui));
            }
        });
    }

    public <T> T context(Class<T> cl) {
        return (ctx.context(cl));
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Glob glob() {
        return (context(Glob.class));
    }

    public Resource getres() {
        return (res.res.get());
    }

    private Random rnd = null;

    public Random mkrandoom() {
        if (rnd == null)
            rnd = new Random();
        return (rnd);
    }

    public GSprite sprite() {
        return (spr);
    }

    public Resource resource() {
        return (res.res.get());
    }

    private GSprite spr = null;

    public GSprite spr() {
        if (spr == null)
            spr = GSprite.create(this, res.res.get(), res.sdt.clone());
        return (spr);
    }

    private List<ItemInfo> cinfo = null;

    public List<ItemInfo> info() {
        if (cinfo == null)
            cinfo = ItemInfo.buildinfo(this, info);
        return (cinfo);
    }

    public Tex longtip() {
        return (new TexI(ItemInfo.longtip(info())));
    }

    public String name() {
        GSprite spr = spr();
        ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info());
        if (nm == null)
            return (null);
        return (nm.str.text);
    }
}
