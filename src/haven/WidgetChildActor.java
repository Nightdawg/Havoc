package haven;

/**
 * Attach this to a widget as a child and let it do its actions you want it to do occasionally
 * @param <T>
 */
public abstract class WidgetChildActor<T extends Widget> extends Widget{
    public T parent;
    
    public WidgetChildActor(T parent) {
        this.parent = parent;
    }
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
        act(parent);
    }
    
    public abstract void act(T parent);
}
