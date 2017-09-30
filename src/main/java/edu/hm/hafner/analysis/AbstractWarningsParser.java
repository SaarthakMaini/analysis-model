package edu.hm.hafner.analysis;

import javax.annotation.CheckReturnValue;
import java.io.Reader;
import java.io.Serializable;
import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Functions;

import edu.hm.hafner.util.Ensure;

/**
 * Parses an input stream for compiler warnings and returns the found issues. If your parser is based on a regular
 * expression you can extend from the existing base classes {@link RegexpLineParser} or {@link RegexpDocumentParser}.
 *
 * @author Ullrich Hafner
 * @see RegexpLineParser
 * @see RegexpDocumentParser
 * @see edu.hm.hafner.analysis.parser.CppLintParser
 * @see edu.hm.hafner.analysis.parser.EclipseParser
 * @see edu.hm.hafner.analysis.parser.StyleCopParser
 */
public abstract class AbstractWarningsParser implements Serializable {
    private static final long serialVersionUID = 8466657735514387654L;

    public static final String DEPRECATION = "Deprecation";
    public static final String PROPRIETARY_API = "Proprietary API";

    private final String id;

    private transient Function<String, String> transformer;

    /**
     * Creates a new instance of {@link AbstractWarningsParser}.
     *
     * @param id ID of the parser
     */
    protected AbstractWarningsParser(final String id) {
        this.id = id;
    }

    /**
     * Parses the specified input stream for issues.
     *
     * @param reader the reader to get the text from
     * @return the parsed issues
     * @throws ParsingException         Signals that during parsing a non recoverable error has been occurred
     * @throws ParsingCanceledException Signals that the parsing has been aborted by the user
     */
    public abstract Issues parse(Reader reader) throws ParsingCanceledException, ParsingException;

    @Override
    public String toString() {
        return String.format("%s (%s)", getId(), getClass().getSimpleName());
    }

    /**
     * Returns the ID of this parser.
     *
     * @return the ID of this parser
     */
    public String getId() {
        return id;
    }

    /**
     * Converts a string line number to an integer value. If the string is not a valid line number, then 0 is returned
     * which indicates a Issue at the top of the file.
     *
     * @param lineNumber the line number (as a string)
     * @return the line number
     */
    protected final int getLineNumber(final String lineNumber) {
        return convertLineNumber(lineNumber);
    }

    /**
     * Converts a string line number to an integer value. If the string is not a valid line number, then 0 is returned
     * which indicates a Issue at the top of the file.
     *
     * @param lineNumber the line number (as a string)
     * @return the line number
     */
    public static int convertLineNumber(final String lineNumber) {
        if (StringUtils.isNotBlank(lineNumber)) {
            try {
                return Integer.parseInt(lineNumber);
            }
            catch (NumberFormatException ignored) {
                // ignore and return 0
            }
        }
        return 0;
    }

    /**
     * Classifies the warning message: tries to guess a category from the warning message.
     *
     * @param message the message to check
     * @return warning category, empty string if unknown
     */
    public static String classifyWarning(final String message) {
        if (StringUtils.contains(message, "proprietary")) {
            return PROPRIETARY_API;
        }
        if (StringUtils.contains(message, "deprecated")) {
            return DEPRECATION;
        }
        return StringUtils.EMPTY;
    }

    /**
     * Returns a category for the current warning. If the provided category is not empty, then a capitalized string is
     * returned. Otherwise the category is obtained from the specified message text.
     *
     * @param group   the warning category (might be empty)
     * @param message the warning message
     * @return the actual category
     */
    public static String classifyIfEmpty(final String group, final String message) {
        String category = StringUtils.capitalize(group);
        if (StringUtils.isEmpty(category)) {
            category = classifyWarning(message);
        }
        return category;
    }

    /**
     * Sets an optional line transformer. This transformer will be called during parsing: each line of the input file
     * will be transformed using the provided transformer before it is handed over to the actual parser.
     *
     * @param transformer the transformer
     */
    public void setTransformer(final Function<String, String> transformer) {
        Ensure.that(transformer).isNotNull();

        this.transformer = transformer;
    }

    /**
     * Returns the specified line transformer. If no such transformer has been set then an transformer is returned that
     * does not modify the input lines.
     *
     * @return the transformer to use
     */
    public Function<String, String> getTransformer() {
        return ObjectUtils.defaultIfNull(transformer, Functions.identity());
    }

    /**
     * Returns a new issue builder that has the ID of this warning set as type property.
     *
     * @return a new issue builder
     */
    @CheckReturnValue
    protected IssueBuilder issueBuilder() {
        return new IssueBuilder().setType(getId());
    }
}
