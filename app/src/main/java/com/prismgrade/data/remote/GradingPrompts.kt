package com.prismgrade.data.remote

/**
 * The prompts that drive an inspection. Both modes ask for the same JSON shape
 * so a single parser and a single UI serve them.
 */
object GradingPrompts {

    /** The exact object we ask Claude to return. Kept on one line for the prompt. */
    private const val SCHEMA =
        """{"identification":{"name":"","setName":"","year":"","cardNumber":"","confidence":"high|medium|low"},""" +
            """"valueEstimate":{"low":0,"high":0,"currency":"USD","basis":"one short sentence"},""" +
            """"subgrades":{"centering":{"score":0,"note":""},"corners":{"score":0,"note":""},""" +
            """"edges":{"score":0,"note":""},"surface":{"score":0,"note":""}},""" +
            """"overallGrade":0,"gradeLabel":"","authenticity":{"flag":"no concerns|review suggested","note":""},""" +
            """"findings":["",""]}"""

    const val SYSTEM = "You are a meticulous trading card authenticator and grader, working to the " +
        "standards used by PSA, BGS and CGC. You answer only with the JSON object you are asked for."

    private const val JSON_RULES = "Scores are integers 1 to 10. Reply with ONLY one JSON object, " +
        "no other text, shaped exactly like this:\n$SCHEMA\n" +
        "Never mention that you are an AI and add no text outside the JSON object."

    /** Photo mode: one or two images accompany this text. */
    fun forPhotos(hasBack: Boolean): String = buildString {
        append("You are examining photos of ONE physical trading card. ")
        append(
            if (hasBack) "The first image is the front, the second is the back. "
            else "Only the front is pictured. ",
        )
        append("Study centering (compare border widths on all four sides and express it like \"60/40\"), ")
        append("corners (sharpness, whitening, fraying), edges (chipping, roughness), ")
        append("surface (scratches, print lines, glare or holo scuffing, focus and registration), ")
        append("and any sign of alteration, trimming or recoloring.\n\n")
        append("Identify the card, estimate its current market value in the condition shown, ")
        append("and grade it. If you cannot identify it confidently, give your best guess and ")
        append("set confidence to \"low\".\n\n")
        append(JSON_RULES)
    }

    /**
     * Description mode: no images. Written to make the model say where the
     * description leaves it guessing rather than inventing detail.
     */
    fun forDescription(card: String, condition: String): String = buildString {
        append("You have NO photos — only the owner's written description. Reason from what they ")
        append("wrote, and say plainly in your notes where the description leaves you uncertain.\n\n")
        append("The card:\n")
        append(card.trim())
        append("\n\nCondition as described by the owner:\n")
        append(condition.trim().ifBlank { "(they did not describe the condition)" })
        append("\n\nIdentify the card, estimate its market value in that condition, and score ")
        append("centering, corners, edges and surface from what was described. Where the owner ")
        append("said nothing about a category, assume ordinary shelf-worn condition for a card of ")
        append("that age, score it conservatively, and say so in that category's note.\n\n")
        append(JSON_RULES)
    }
}
