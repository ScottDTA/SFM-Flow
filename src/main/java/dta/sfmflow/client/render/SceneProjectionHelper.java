package dta.sfmflow.client.render;

import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles mathematical 3D-to-2D coordinate projections and orbital visibility checks [3].
 */
@OnlyIn(Dist.CLIENT)
public final class SceneProjectionHelper {

	public record ProjectedVec(float x, float y, float z) {}
	public record ProjectedFace(Direction face, ProjectedVec proj) {}

	private SceneProjectionHelper() {}

	/**
	 * Projects 3D block face coordinates onto 2D screen positions based on camera rotations [3].
	 */
	public static ProjectedVec getFaceScreenCoords(Direction face, float yaw, float pitch, float scale, int centerX, int centerY) {
		float x = face.getStepX() * 0.5F;
		float y = face.getStepY() * 0.5F;
		float z = face.getStepZ() * 0.5F;

		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);

		double x1 = x * Math.cos(yawRad) + z * Math.sin(yawRad);
		double y1 = y;
		double z1 = -x * Math.sin(yawRad) + z * Math.cos(yawRad);

		double x2 = x1;
		double y2 = y1 * Math.cos(pitchRad) - z1 * Math.sin(pitchRad);
		double z2 = y1 * Math.sin(pitchRad) + z1 * Math.cos(pitchRad);

		float screenX = centerX + (float) (x2 * scale);
		float screenY = centerY - (float) (y2 * scale);

		return new ProjectedVec(screenX, screenY, (float) z2);
	}

	/**
	 * Resolves sorted block faces based on distance to camera [3].
	 */
	public static List<Direction> getVisibleFaces(float yaw, float pitch, float scale, int centerX, int centerY) {
		List<ProjectedFace> faceList = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			ProjectedVec proj = getFaceScreenCoords(dir, yaw, pitch, scale, centerX, centerY);
			faceList.add(new ProjectedFace(dir, proj));
		}

		faceList.sort((f1, f2) -> Float.compare(f2.proj().z(), f1.proj().z()));

		List<Direction> visibleFaces = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			visibleFaces.add(faceList.get(i).face());
		}
		return visibleFaces;
	}
}