package org.biofid.deep_eos;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import jep.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.hucompute.textimager.uima.base.JepAnnotator;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;

/**
 * Created on 09.10.19.
 */
public class DeepEosTagger extends JepAnnotator {
	
	public static final String PARAM_MODEL_NAME = "modelname";
	@ConfigurationParameter(
			name = PARAM_MODEL_NAME,
			defaultValue = "de"
	)
	private String modelname;
	
	public static final String PARAM_VERBOSE = "verbose";
	@ConfigurationParameter(
			name = PARAM_VERBOSE,
			defaultValue = "false"
	)
	private Boolean verbose;
	
	private static final String[] resourceFiles = new String[]{"python/model_deepeos.py", "python/utils_deepeos.py","python/__init__.py"};
	private Path tempFolder;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		if (envName == null || envName.isEmpty()) {
			envName = "textimager_deepeos_py362";
		}
		if (envPythonVersion == null || envPythonVersion.isEmpty()) {
			envPythonVersion = "3.6";
		}
		if (envDepsConda == null || envDepsConda.isEmpty()) {
			envDepsConda = "";
		}
		if (envDepsPip == null || envDepsPip.isEmpty()) {
			envDepsPip = "tensorflow==1.5.0 uarray==0.6.0 keras==2.1.5 h5py==2.10.0";
		}
		if (condaVersion == null || condaVersion.isEmpty()) {
			condaVersion = "py37_4.8.3";
		}
		
		initConda();
		try {
			tempFolder = Files.createTempDirectory(this.getClass().getSimpleName());
			Properties modelProperties = loadModelProperties();
			extractResources();
			ModelConfig modelConfig = new ModelConfig(modelProperties, modelname);
			interpreter.exec("import os");
			interpreter.exec("import sys");
			
			interpreter.exec("sys.path.append('" + tempFolder.toAbsolutePath().toString() + "/python/')");
			interpreter.exec("from model_deepeos import DeepEosModel");
			interpreter.exec(String.format("model = DeepEosModel(model_base_path='%s', window_size=%d)", modelConfig.basePath, modelConfig.windowSize));
		} 
		catch (IOException | JepException e) {
			throw new ResourceInitializationException(e);
		}
		
	}
	
	private void extractResources() throws IOException {
		for (String fileName : resourceFiles) {
			Path outPath = Paths.get(tempFolder.toString(), fileName);
			Files.createDirectories(outPath.getParent());
			try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
				FileUtils.copyInputStreamToFile(Objects.requireNonNull(resourceAsStream), outPath.toFile());
			}
		}
	}
	
	private Properties loadModelProperties() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("models.properties")) {
			properties.load(input);
		}
		return properties;
	}
	
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		String documentText = jCas.getDocumentText();
		try {
			ArrayList<Long> result = (ArrayList<Long>) interpreter.invoke("model.tag", documentText);
			int begin = 0;
			for (int i = 0; i < result.size(); i++) {
				Long end = result.get(i);
				Sentence sentence = new Sentence(jCas, begin, Math.toIntExact(end) + 1);
				sentence.setId(String.valueOf(i));
				jCas.addFsToIndexes(sentence);
				begin = Math.toIntExact(end) + 2;
			}
			Sentence sentence = new Sentence(jCas, begin, jCas.getDocumentText().length());
			sentence.setId(String.valueOf(result.size()));
			jCas.addFsToIndexes(sentence);
			
			if (verbose) {
				System.out.println();
				for (Sentence sent : JCasUtil.select(jCas, Sentence.class)) {
					System.out.print(sent);
					System.out.println("   text: \"" + sent.getCoveredText() + "\"");
				}
			}
		} catch (JepException | ClassCastException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	@Override
	public void destroy() {
		try {
			if (tempFolder != null) {
				FileSystemUtils.deleteRecursively(tempFolder.toFile());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
	
	private static class ModelConfig {
		final String basePath;
		final String modelPath;
		final String vocabPath;
		final int windowSize;
		
		
		ModelConfig(Properties properties, String modelName) {
			modelPath = properties.getProperty(modelName + ".model");
			basePath = StringUtils.substringBefore(StringUtils.substringBefore(modelPath, ".model"), ".hdf5");
			if (properties.containsKey(modelName + ".vocab")) {
				vocabPath = properties.getProperty(modelName + ".vocab");
			} else {
				vocabPath = StringUtils.substringAfterLast(modelPath, ".") + ".vocab";
			}
			windowSize = Integer.parseInt(properties.getProperty(modelName + ".window_size", "5"));
		}
	}
}
