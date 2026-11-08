package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class QuestContractPersistenceTest {
    private static CompoundTag legacyContract() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", UUID.randomUUID().toString());
        tag.putUUID("IssuerId", UUID.randomUUID());
        tag.putString("IssuerName", "Merc");
        tag.putString("IssuerType", "merc");
        tag.putString("Type", "elimination");
        tag.putString("Status", "active");
        tag.putInt("TargetCount", 6);
        tag.putInt("Progress", 2);
        return tag;
    }

    @Test
    public void oldContractsLoadWithoutEncounterState() {
        QuestContract contract = QuestContract.fromTag(legacyContract());
        assertEquals(0, contract.huntSpawnedCount());
        assertEquals(2, contract.progress());
        assertEquals(6, contract.targetCount());
    }

    @Test
    public void spawnedCountSurvivesReloadWithoutChangingProgress() {
        QuestContract contract = QuestContract.fromTag(legacyContract());
        contract.recordHuntSpawn();
        contract.recordHuntSpawn();
        QuestContract restored = QuestContract.fromTag(contract.toTag());
        assertEquals(2, restored.huntSpawnedCount());
        assertEquals(contract.id(), restored.id());
        assertEquals(contract.status(), restored.status());
        assertEquals(2, restored.progress());
        restored.recordHuntSpawn();
        assertEquals(3, QuestContract.fromTag(restored.toTag()).huntSpawnedCount());
    }
}
