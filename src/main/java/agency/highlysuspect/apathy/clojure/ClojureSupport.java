package agency.highlysuspect.apathy.clojure;

import agency.highlysuspect.apathy.Init;
import clojure.java.api.Clojure;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClojureSupport {
	public static final Logger LOG = LogManager.getLogger(Init.MODID + "-clojure");
	
	public static void onInitialize(Path clojureConfigPath) {
		//Since Clojure is loaded it's safe to classload now.
		//Point the clojure proxy at the method in the clojure api class.
		Init.clojureProxy = Api::allowedToTargetPlayer;
		
		//Load the standard library stored in the mod .jar .
		loadIntoClojure("Loading apathy's standard library, might take a little bit",
			Init.class.getClassLoader().getResourceAsStream("apathy-startup.clj"), true);
		
		//Load the apathy.clj file, and reload it on every /reload.
		Init.installReloadAndRunNow("clojure-config", () -> {
			try {
				if(Files.exists(clojureConfigPath)) {
					loadIntoClojure("Loading apathy.clj config", Files.newInputStream(clojureConfigPath), false);
				} else {
					LOG.error("Looked for apathy Clojure config at " + clojureConfigPath + ", but the file does not exist.");
				}
			} catch (IOException e) {
				throw new RuntimeException("Problem initializing Clojure config script", e);
			}
		});
		
		//Load apathy/____.clj files from datapacks.
		Init.installReload("clojure-datapacks", (manager) -> {
			try {
				for(Identifier id : manager.findResources(Init.MODID, (path) -> path.endsWith(".clj"))) {
					Resource resource = manager.getResource(id);
					loadIntoClojure("Loading datapack Clojure script from " + id.toString(), resource.getInputStream(), false);
				}
			} catch (IOException e) {
				LOG.error("Problem loading list of Clojure scripts", e);
			}
		});
	}
	
	private static void loadIntoClojure(String message, InputStream yea, boolean hardFail) {
		try(InputStreamReader reader = new InputStreamReader(yea)) {
			LOG.info(message);
			Clojure.var("clojure.core", "load-reader").invoke(reader);
			LOG.info("Success!");
		} catch (RuntimeException | IOException e) {
			LOG.error("Failure.");
			if(hardFail) {
				throw new RuntimeException(e);
			} else {
				LOG.error(e);
				LOG.error("Problem loading the Clojure file. Attempting to continue.");
			}
		}
	}
}
