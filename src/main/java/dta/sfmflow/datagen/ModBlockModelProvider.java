package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Custom block model provider that generates single-face models for multi-sided network blocks [3].
 * Utilizes direct raw JSON mutations to output explicit scissor-matched cullface mappings [3].
 */
public class ModBlockModelProvider extends BlockModelProvider {

    /**
     * Instantiates the custom model provider [3].
     *
     * @param output the pack output folder [3]
     * @param existingFileHelper the helper for verifying existing assets [3]
     */
    public ModBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, SFMFlow.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Register the Off and On multi-sided indicator faces
        registerSingleFaceModel("redstone_emitter_face_off", "block/redstone_emitter_side_off", "block/redstone_emitter_face_off");
        registerSingleFaceModel("redstone_emitter_face_on", "block/redstone_emitter_side_on", "block/redstone_emitter_face_on");
    }

    /**
     * Programmatically constructs a single culled-face model using raw JSON elements array injections [3].
     *
     * @param modelName the filename to output [3]
     * @param sideTexturePath texture path representing unpowered sides [3]
     * @param faceTexturePath texture path representing active face indicators [3]
     */
    private void registerSingleFaceModel(String modelName, String sideTexturePath, String faceTexturePath) {
        ResourceLocation sideTexture = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, sideTexturePath);
        ResourceLocation faceTexture = ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, faceTexturePath);

        // Define parent template cleanly using modern non-deprecated ResourceLocations [3]
        var builder = withExistingParent(modelName, ResourceLocation.withDefaultNamespace("block/block"))
                .texture("particle", sideTexture)
                .texture("face", faceTexture);

        JsonObject elementObj = new JsonObject();
        
        // Define coordinates: "from": [0, 0, 0]
        JsonArray fromArray = new JsonArray();
        fromArray.add(0); fromArray.add(0); fromArray.add(0);
        elementObj.add("from", fromArray);

        // Define coordinates: "to": [16, 16, 16]
        JsonArray toArray = new JsonArray();
        toArray.add(16); toArray.add(16); toArray.add(16);
        elementObj.add("to", toArray);

        // Define specific face direction: "faces": { "up": { ... } }
        JsonObject facesObj = new JsonObject();
        JsonObject upObj = new JsonObject();
        
        JsonArray uvArray = new JsonArray();
        uvArray.add(0); uvArray.add(0); uvArray.add(16); uvArray.add(16);
        upObj.add("uv", uvArray);
        upObj.addProperty("texture", "#face");
        upObj.addProperty("cullface", "up");
        
        facesObj.add("up", upObj);
        elementObj.add("faces", facesObj);

        // Embed the element into the list array
        JsonArray elementsArray = new JsonArray();
        elementsArray.add(elementObj);
        
        // Append raw elements array onto the builder JSON payload [3]
        builder.toJson().add("elements", elementsArray);
    }
}