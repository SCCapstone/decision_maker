package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.CategoriesManager;
import utilities.IOStreamsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static utilities.IOStreamsHelper.parseInput;

public class CategoriesGetter implements RequestStreamHandler {
    private final CategoriesManager categoriesManager = new CategoriesManager();

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {

        try {
            Map<String, Object> jsonInput = parseInput(inputStream);
            String username = null;
            if(jsonInput.containsKey("username")){
                username = jsonInput.get("username").toString();
            }
            if(username!=null){
                String jsonPairsData = this.categoriesManager.getJsonStringOfTableDataForFrontEnd();

                IOStreamsHelper.writeToOutput(outputStream, jsonPairsData);
            }

        } catch (Exception e) {
            //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
            IOStreamsHelper.writeToOutput(outputStream, "Unable to handle request.\n");
        }
    }
}
