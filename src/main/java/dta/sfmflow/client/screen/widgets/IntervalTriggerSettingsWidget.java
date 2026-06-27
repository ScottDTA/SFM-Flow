package dta.sfmflow.client.screen.widgets;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.api.client.widget.FlowWidgetText;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.util.Color;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom settings panel widget dedicated to configuring the Interval Trigger parameters [3].
 * Houses TimeUnit selection cycle buttons and range-clamped delay duration slider controls [3].
 */
@OnlyIn(Dist.CLIENT)
public class IntervalTriggerSettingsWidget extends AbstractFlowWidget
 {
  private final FlowWidgetContainer container;
  private final IntervalTriggerComponent component;

  public IntervalTriggerSettingsWidget(FlowWidgetContainer container, IntervalTriggerComponent component)
   {
    super(container.getX(), container.getY(), component.getVisualWidth(), component.getVisualHeight(), Component.literal("Interval Settings"));
    this.container = container;
    this.component = component;

    final IntervalSlider[] sliderHolder = new IntervalSlider[1];

    // Instantiate TimeUnit CycleButton using displayOnlyValue() to clean up formatting [3]
    CycleButton<IntervalTriggerComponent.TimeUnit> cycleButton = CycleButton.builder(IntervalTriggerComponent.TimeUnit::getDisplayName)
        .withValues(IntervalTriggerComponent.TimeUnit.values())
        .withInitialValue(component.getTimeUnit())
        .displayOnlyValue()
        .create(getX() + 22, getY() + 36, 80, 20, Component.literal("TimeUnit"), (btn, value) -> {
            component.setTimeUnit(value);

            // Clamp value according to scale boundaries
            int minVal = (value == IntervalTriggerComponent.TimeUnit.TICKS) ? dta.sfmflow.ServerConfig.MIN_INTERVAL_TICKS.get() : 1;
            int maxVal = (value == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
            component.setIntervalValue(Mth.clamp(component.getIntervalValue(), minVal, maxVal));

            container.getParent().getMenu().getManagerBlockEntity().setChanged();
            
            // Transmit changes to the server
            sendSettingsUpdate();

            if (sliderHolder[0] != null)
             {
              sliderHolder[0].refresh();
             }
        });

    // Label directly above the TimeUnit CycleButton - configured with dynamic text color [3]
    this.children.add(new FlowWidgetText(
        container.getParent().getFont(), 
        getX() + 22, 
        getY() + 25, 
        80, 
        10, 
        Component.translatable("gui.sfmflow.time_unit"), 
        1.0F, 
        true,
        () -> {
            Color mask = component.getColorMask();
            return mask != null ? mask.getHexTextColor() : 4210752;
        }
    ));

    // Durational speed control slider
    IntervalSlider slider = new IntervalSlider(getX() + 12, getY() + 62, 100, 20, component, container, this);
    sliderHolder[0] = slider;

    this.children.add(new ApiWidgetAdapter<>(cycleButton, component::getColorMask));
    this.children.add(new ApiWidgetAdapter<>(slider, component::getColorMask));
   }

  private void sendSettingsUpdate()
   {
    CompoundTag nbt = new CompoundTag();
    component.saveData(nbt);
    net.minecraft.core.BlockPos pos = container.getParent().getMenu().getManagerBlockEntity().getBlockPos();
    PacketDistributor.sendToServer(new SaveComponentSettings(pos, component.getId(), nbt));
   }

  @Override
  protected void renderComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
   {
    for (GuiEventListener child : children)
     {
      if (child instanceof AbstractFlowWidget widget)
       {
        widget.visible = this.visible;
        widget.active = this.active;
        widget.render(guiGraphics, mouseX, mouseY, partialTick);
       }
     }
   }

  @Override
  public void setX(int x)
   {
    int dif = this.getX() - x;
    super.setX(x);
    updateChildrenXPositions(dif);
   }

  @Override
  public void setY(int y)
   {
    int dif = this.getY() - y;
    super.setY(y);
    updateChildrenYPositions(dif);
   }

  /**
   * Standalone client-side Minecraft slider button subclass mapped to the Interval trigger settings [3].
   */
  @OnlyIn(Dist.CLIENT)
  private static class IntervalSlider extends AbstractSliderButton
   {
    private final IntervalTriggerComponent component;
    private final FlowWidgetContainer container;
    private final IntervalTriggerSettingsWidget settingsWidget;

    public IntervalSlider(int x, int y, int width, int height, IntervalTriggerComponent component, FlowWidgetContainer container, IntervalTriggerSettingsWidget settingsWidget)
     {
      super(x, y, width, height, Component.literal("Interval"), getInitialValueProgress(component));
      this.component = component;
      this.container = container;
      this.settingsWidget = settingsWidget;
      this.updateMessage();
     }

    private static double getInitialValueProgress(IntervalTriggerComponent comp)
     {
      int val = comp.getIntervalValue();
      int min = getMinLimit(comp);
      int max = getMaxLimit(comp);
      return (double) (val - min) / (double) (max - min);
     }

    private static int getMinLimit(IntervalTriggerComponent comp)
     {
      return (comp.getTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) 
          ? dta.sfmflow.ServerConfig.MIN_INTERVAL_TICKS.get() : 1;
     }

    private static int getMaxLimit(IntervalTriggerComponent comp)
     {
      return (comp.getTimeUnit() == IntervalTriggerComponent.TimeUnit.TICKS) ? 100 : 60;
     }

    public void refresh()
     {
      this.value = getInitialValueProgress(this.component);
      this.updateMessage();
     }

    @Override
    protected void updateMessage()
     {
      int min = getMinLimit(this.component);
      int max = getMaxLimit(this.component);
      int val = min + (int) Math.round(this.value * (max - min));
      setMessage(Component.literal(val + " " + this.component.getTimeUnit().getDisplayName().getString()));
     }

    @Override
    protected void applyValue()
     {
      int min = getMinLimit(this.component);
      int max = getMaxLimit(this.component);
      int val = min + (int) Math.round(this.value * (max - min));
      this.component.setIntervalValue(val);
      this.container.getParent().getMenu().getManagerBlockEntity().setChanged();
     }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
     {
      if (button == 0)
       {
        this.settingsWidget.sendSettingsUpdate();
       }
      return super.mouseReleased(mouseX, mouseY, button);
     }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
     {
      if (this.visible && this.active && this.isMouseOver(mouseX, mouseY))
       {
        int min = getMinLimit(this.component);
        int max = getMaxLimit(this.component);
        int val = this.component.getIntervalValue();
        int step = 1;
        int newVal = Mth.clamp(val + (scrollY > 0 ? step : -step), min, max);

        if (newVal != val)
         {
          this.component.setIntervalValue(newVal);
          this.value = (double) (newVal - min) / (double) (max - min);
          this.updateMessage();
          this.container.getParent().getMenu().getManagerBlockEntity().setChanged();
          this.settingsWidget.sendSettingsUpdate();
         }
        return true;
       }
      return false;
     }
   }
 }