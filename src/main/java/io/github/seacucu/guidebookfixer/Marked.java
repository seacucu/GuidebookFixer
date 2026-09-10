package io.github.seacucu.guidebookfixer;

/**
 * Lets one Mixin tell another that the recipe it just handed over is not the
 * one the page asked for.
 *
 * <p>The substitution happens where the recipe is looked up and the badge is
 * drawn where the page is painted, and in some books those are different
 * classes in a hierarchy. Mixin adds this interface to the target so the two
 * can talk without a side table keyed on page instances.
 */
public interface Marked {

    void guidebookfixer$markSubstituted(boolean packAuthored);

    boolean guidebookfixer$isSubstituted();

    boolean guidebookfixer$isPackAuthored();
}
