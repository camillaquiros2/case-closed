package caseclosed.data

import caseclosed.model.CaseData
import caseclosed.model.CaseFile
import caseclosed.model.CasePack
import caseclosed.model.CaseSolution
import caseclosed.model.ClueDef
import caseclosed.model.DocReference
import caseclosed.model.FileSection
import caseclosed.model.ImageRef
import caseclosed.model.Item
import caseclosed.model.PhoneMessage
import caseclosed.model.Question
import caseclosed.model.Suspect
import caseclosed.model.DiscoveryDef

private const val SCENE = "images/cases/case_002/scenes/scene_pier.png"

internal val case002 = CasePack(
    initialDiscoveredItemIds = listOf("item_ledger"),
    data = CaseData(
        id = "case_002",
        caseNumber = "CC-1925-009",
        title = "Midnight on Pier 9",
        subtitle = "Rain on the warehouse door",
        briefing = "Night watchman Eddie Crowe was found inside Warehouse 9 after the rain began. A smuggling ledger was folded in his coat. Two men had reason to stop that book from reaching the police: the dock boss, and the harbor master who keeps the keys.",
        victim = "Eddie Crowe, night watchman",
        location = "Warehouse 9, Harbor District",
        date = "3 March 1925",
        files = listOf(
            CaseFile(
                id = "file_201",
                title = "Harbor Incident Report",
                documentNumber = "CC-1925-009-A",
                date = "3 March 1925",
                thumbnail = SCENE,
                shortDescription = "Where the watchman fell, and when the rain started.",
                relatedSuspectIds = listOf("suspect_malone", "suspect_silas"),
                relatedItemIds = listOf("item_ledger", "item_key"),
                sections = listOf(
                    FileSection(
                        id = "sec_201",
                        heading = "The door",
                        content = "Eddie Crowe was found just inside the Warehouse 9 door. One blow to the temple. He died where he fell. Rain began at 11:30. His coat is wet on the shoulders and dry underneath, so he was inside before the worst of it and did not get up again.",
                        image = ImageRef(SCENE, "Warehouse 9 on Pier 9, photographed in the rain after the body was moved."),
                        references = listOf(
                            DocReference("item", "item_key", "Warehouse key"),
                            DocReference("item", "item_ledger", "Ledger"),
                        ),
                    ),
                    FileSection(
                        id = "sec_205",
                        heading = "The book in the coat",
                        content = "A ledger in the watchman's coat records night payments initialed S.V. One page is copied in Eddie Crowe's own hand, folded as if he meant to carry it off the pier.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_204",
                                text = "A ledger in the watchman's coat records night payments initialed S.V.",
                                description = "The paid column points at the harbor master.",
                            ),
                        ),
                        references = listOf(
                            DocReference("item", "item_ledger", "Contraband ledger"),
                            DocReference("suspect", "suspect_silas", "Captain Silas Vance"),
                        ),
                    ),
                ),
            ),
            CaseFile(
                id = "file_202",
                title = "Coroner's Clock",
                documentNumber = "CC-1925-009-B",
                date = "4 March 1925",
                thumbnail = SCENE,
                shortDescription = "The hour the watchman actually died.",
                relatedSuspectIds = listOf("suspect_malone"),
                relatedItemIds = emptyList(),
                sections = listOf(
                    FileSection(
                        id = "sec_202",
                        heading = "Time of death",
                        content = "Body heat and the dry patch under the coat put the death between 11:40 and 11:55. Not later. Anyone still at a card table after midnight was not standing in that doorway.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_201",
                                text = "Body heat and the dry patch under the coat put the death between 11:40 and 11:55.",
                                description = "The killing window closes before midnight.",
                            ),
                        ),
                    ),
                ),
            ),
            CaseFile(
                id = "file_203",
                title = "Anchor Card-Room Statement",
                documentNumber = "CC-1925-009-C",
                date = "4 March 1925",
                thumbnail = "images/cases/case_002/suspects/suspect_malone.png",
                shortDescription = "Three players swear Malone never left the table.",
                relatedSuspectIds = listOf("suspect_malone"),
                relatedItemIds = emptyList(),
                sections = listOf(
                    FileSection(
                        id = "sec_203",
                        heading = "The table",
                        content = "Frank Malone sat in the Anchor card room from 11:15 until 12:50 and never left the table. The dealer, the barman, and a customs clerk all sign the same times. He was loud. He was watched. He was not on Pier 9.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_202",
                                text = "Frank Malone sat in the Anchor card room from 11:15 until 12:50 and never left the table.",
                                description = "Malone's alibi covers the whole killing window.",
                            ),
                        ),
                        references = listOf(
                            DocReference("suspect", "suspect_malone", "Frank Malone"),
                            DocReference("file", "file_202", "Coroner's clock"),
                        ),
                    ),
                ),
            ),
            CaseFile(
                id = "file_204",
                title = "Dock Office Key Log",
                documentNumber = "CC-1925-009-D",
                date = "3 March 1925",
                thumbnail = "images/cases/case_002/items/item_key.png",
                shortDescription = "Which key slept in the lockbox, and which hook was empty.",
                relatedSuspectIds = listOf("suspect_malone", "suspect_silas"),
                relatedItemIds = listOf("item_key"),
                sections = listOf(
                    FileSection(
                        id = "sec_204",
                        heading = "The hooks",
                        content = "Malone's warehouse key was signed into the dock office lockbox at 9:10 and was still there at 1:00. The harbor master's hook in the tide office was empty all night. The clerk will not say he saw the master key leave. He will say the hook was bare.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_203",
                                text = "The harbor master's hook in the tide office was empty all night.",
                                description = "Silas's key was not where he said it hung.",
                            ),
                        ),
                        references = listOf(
                            DocReference("suspect", "suspect_silas", "Captain Silas Vance"),
                            DocReference("item", "item_key", "Brass key"),
                            DocReference("suspect", "suspect_malone", "Frank Malone"),
                        ),
                    ),
                ),
            ),
        ),
        suspects = listOf(
            Suspect(
                id = "suspect_malone",
                name = "Frank Malone",
                alias = "Nails",
                portrait = "images/cases/case_002/suspects/suspect_malone.png",
                shortDescription = "Dock boss they call Nails. Runs the night crews and the card table. Loud, watched, and angry that anyone touched his books.",
                occupation = "Dock boss",
                knownInformation = "Controls the night gangs on Pier 9. His key was logged in the lockbox. The ledger names payments he would rather keep quiet.",
                alibi = "At the Anchor card room from 11:15 to 12:50. Three witnesses, including a customs clerk, say he never stood up.",
                questions = listOf(
                    Question(
                        id = "q_n1",
                        question = "Where were you when the watchman died?",
                        answer = "At the Anchor. Cards. From a quarter past eleven until nearly one. Ask the table. I never went to Warehouse 9.",
                        revealsClue = "hl_202",
                    ),
                    Question(
                        id = "q_n2",
                        question = "Is this your ledger?",
                        answer = "That book is a clerk's poison. My cut is spoken, not written. I don't initial pages, and I don't kill a man to hide a column.",
                    ),
                    Question(
                        id = "q_n3",
                        question = "Who holds a key to Warehouse 9?",
                        answer = "The watchman, and the harbor master. My key sleeps in the dock office lockbox. The clerk will swear to the signature.",
                    ),
                ),
            ),
            Suspect(
                id = "suspect_silas",
                name = "Captain Silas Vance",
                alias = "The Harbor Master",
                portrait = "images/cases/case_002/suspects/suspect_silas.png",
                shortDescription = "Harbor master. Keeps the warehouse keys, the tide book, and a quiet arrangement with the crews who pay to be unseen.",
                occupation = "Harbor master",
                knownInformation = "One of two master keys to Warehouse 9 hangs in his office. The ledger's paid column is initialed S.V.",
                alibi = "Claims he was on the tide walk from eleven until half past, and that his key never left its hook.",
                questions = listOf(
                    Question(
                        id = "q_s1",
                        question = "Where was your master key tonight?",
                        answer = "On the hook in my office. If it is not there now, someone lifted it. I was on the tide walk from eleven until half past.",
                    ),
                    Question(
                        id = "q_s2",
                        question = "The clerk says the hook was empty. What is in your desk?",
                        answer = "Then search the desk. I keep a spare for storms. That does not put me on the pier. Take it, if you are so sure of a key.",
                        revealsItem = "item_key",
                        requiredDiscoveryIds = listOf("hl_203"),
                    ),
                    Question(
                        id = "q_s3",
                        question = "Who is S.V. in the watchman's ledger?",
                        answer = "Half the harbor has those initials. Eddie Crowe made enemies by copying pages that were not his to copy.",
                        revealsClue = "hl_204",
                    ),
                ),
            ),
        ),
        items = listOf(
            Item(
                id = "item_ledger",
                name = "Contraband Ledger",
                image = "images/cases/case_002/items/item_ledger.png",
                shortNotes = "Night payments initialed S.V. A page is in Eddie's hand.",
                description = "A narrow oilcloth book found in Eddie Crowe's coat. The columns record cargo that never reached the customs shed.",
                findings = "Payments are initialed S.V. One page is copied in the watchman's own writing and folded into the binding, as if he meant to carry it to the police.",
                relatedFileIds = listOf("file_201", "file_204"),
            ),
            Item(
                id = "item_key",
                name = "Warehouse Skeleton Key",
                image = "images/cases/case_002/items/item_key.png",
                shortNotes = "Brass master key. Varnish on the bow matches the harbor master's desk.",
                description = "A heavy brass skeleton key recovered from the top drawer of Captain Vance's desk. It opens Warehouse 9.",
                findings = "The teeth match the Warehouse 9 lock. Varnish on the bow matches the harbor master's desk, not the dock office lockbox. Malone's key was still signed in.",
                relatedFileIds = listOf("file_204", "file_201"),
            ),
        ),
        solution = CaseSolution(
            guiltySuspectId = "suspect_silas",
            requiredEvidenceIds = listOf("item_ledger", "item_key", "hl_202"),
            minEvidenceCount = 3,
            explanation = "Captain Silas Vance struck Eddie Crowe inside Warehouse 9 before midnight. Malone was at cards the entire hour. The ledger in the watchman's coat names S.V., and the harbor master's key was off its hook and in his desk. Eddie was taking the book to the police. Silas closed the account.",
            failureHints = mapOf(
                "suspect_malone" to "Nails was under three pairs of eyes at the Anchor when the watchman fell. A loud man with an alibi is still an alibi. Ask who keeps the keys.",
                "suspect_silas" to "The harbor master had the hour and the empty hook. Pin the ledger, the key from his desk, and the card-room clock before you charge him.",
            ),
        ),
        phoneMessages = listOf(
            PhoneMessage(
                id = "msg_201",
                caller = "Inspector Harlan Crowe",
                time = "12:10 a.m.",
                message = "Watchman Eddie Crowe is down inside Warehouse 9. Rain started at half past eleven. Malone's crew is already shouting. Hold the harbor master too.",
                unread = true,
            ),
            PhoneMessage(
                id = "msg_202",
                caller = "Coroner Ada Quinn",
                time = "2:40 a.m.",
                message = "One blow to the temple. He died where he fell, just inside the door, between twenty to midnight and five to midnight. Not later. Do not let a midnight alibi confuse you.",
                unread = true,
            ),
        ),

        discoveries = listOf(
            DiscoveryDef("item_ledger"),
            DiscoveryDef(
                "item_key",
                requiredDiscoveryIds = listOf("hl_203"),
            ),
            DiscoveryDef("hl_201"),
            DiscoveryDef("hl_202"),
            DiscoveryDef("hl_203"),
            DiscoveryDef(
                "hl_204",
                requiredDiscoveryIds = listOf("item_ledger"),
            ),
        ),
    ),
)
