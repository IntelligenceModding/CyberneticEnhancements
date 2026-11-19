package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class MedTechEntity extends AbstractCityNpcEntity {
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.treatmentGoal(this));
    }
    public MedTechEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public NpcType npcType() {
        return NpcType.MED_TECH;
    }
}
