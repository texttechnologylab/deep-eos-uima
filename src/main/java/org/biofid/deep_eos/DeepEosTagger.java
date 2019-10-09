package org.biofid.deep_eos;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import jep.*;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created on 09.10.19.
 */
public class DeepEosTagger extends CasConsumer_ImplBase {
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(
			name = PARAM_LANGUAGE,
			defaultValue = "de"
	)
	private String language;
	
	private Interpreter interp;
	
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			Map<String, ModelConfig> modelConfigHashMap = loadLangMap();
			if (!modelConfigHashMap.containsKey(language)) {
				throw new Exception("The language '" + language + "' is not a valid DeepEOS model language!");
			} else {
				ModelConfig modelConfig = modelConfigHashMap.get(language);
				PyConfig config = new PyConfig();
				config.setPythonHome(Paths.get(System.getenv("HOME")+"/.conda/envs/keras/").toAbsolutePath().toString());
				MainInterpreter.setInitParams(config);
				
				MainInterpreter.setJepLibraryPath(System.getenv("HOME")+"/.conda/envs/keras/lib/python3.7/site-packages/jep/libjep.so");
				interp = new SharedInterpreter();
				interp.exec("import os");
				interp.exec("import sys");
				interp.exec("sys.path.append('src/main/python')"); // FIXME: fix this relative path
				interp.exec("from model import DeepEosModel");
				interp.exec(String.format("model = DeepEosModel(model_base_path='%s', window_size=%d)", modelConfig.path, modelConfig.windowSize));
			}
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}
	
	private Map<String, ModelConfig> loadLangMap() throws IOException {
		HashMap<String, ModelConfig> modelConfigHashMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("models.map")))) {
			for (String s : reader.lines().collect(Collectors.toList())) {
				String[] arr = s.split("\\s+", 3);
				modelConfigHashMap.put(arr[0], new ModelConfig(Paths.get(arr[1]).toAbsolutePath().toString(), Integer.parseInt(arr[2])));
			}
		}
		return modelConfigHashMap;
	}
	
	@Override
	public void process(CAS cas) throws AnalysisEngineProcessException {
		String documentText = cas.getDocumentText();
		try {
			JCas jCas = cas.getJCas();
			ArrayList<Long> result = (ArrayList<Long>) interp.invoke("model.tag", documentText);
			int begin = 0;
			for (Long end : result) {
				Sentence sentence = new Sentence(jCas, begin, Math.toIntExact(end));
				jCas.addFsToIndexes(sentence);
				begin = Math.toIntExact(end);
			}
			Sentence sentence = new Sentence(jCas, begin, jCas.getDocumentText().length());
			jCas.addFsToIndexes(sentence);
			
			System.out.println(JCasUtil.select(jCas, Sentence.class));
		} catch (JepException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (CASException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void destroy() {
		try {
			this.interp.close();
		} catch (JepException e) {
			e.printStackTrace();
		}
		super.destroy();
	}
	
	private static class ModelConfig {
		final String path;
		final int windowSize;
		
		ModelConfig(String path, int windowSize) {
			this.path = path;
			this.windowSize = windowSize;
		}
	}
}
