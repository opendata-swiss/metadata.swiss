package swiss.opendata.piveau.pipe.module.importing.showcases;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import io.piveau.jena.ModelExtract;
import io.piveau.jena.StatementBoundaryBase;

public class ShowcaseExtractor {

    private final ModelExtract extractor;

    public ShowcaseExtractor() {
        this.extractor = new ModelExtract(new StatementBoundaryBase() {
            @Override
            public boolean stopAt(Statement s) {

                // Define a boundary: stop traversing if the object is a named URI resource.
                // This ensures we fully extract nested blank nodes but skip other independent resources.
                return s.getObject().isURIResource();
            }
        });
    }

    public Model extract(Resource showcase, Model sourceModel) {
        return extractor.extract(showcase, sourceModel);
    }
}
