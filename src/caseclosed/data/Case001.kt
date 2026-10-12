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

private const val SCENE = "images/cases/case_001/scenes/scene_manor_study.png"

internal val case001 = CasePack(
    initialDiscoveredItemIds = listOf("item_brandy", "item_pillbox"),
    data = CaseData(
        id = "case_001",
        caseNumber = "CC-1924-014",
        title = "El envenenamiento de la Mansión Blackwood",
        subtitle = "Una amarga noche en el estudio",
        briefing = "Lord Arthur Blackwood fue encontrado muerto sobre el escritorio de su estudio poco después de las once. La forense sospecha de un alcaloide vegetal de acción rápida. Cuatro personas permanecían en la casa. Un nuevo testamento debía firmarse a medianoche. Nadie saldrá de la mansión hasta que puedas identificar la mano que tocó la copa.",
        victim = "Lord Arthur Blackwood",
        location = "Mansión Blackwood, Devon",
        date = "14 de noviembre de 1924",

        files = listOf(
            CaseFile(
                id = "file_001",
                title = "Informe preliminar de la forense",
                documentNumber = "CC-1924-014-A",
                date = "15 de noviembre de 1924",
                thumbnail = SCENE,
                shortDescription = "Las pupilas, el pulso y el veneno que no estaba en el pastillero.",
                relatedSuspectIds = listOf("suspect_reid"),
                relatedItemIds = listOf("item_brandy", "item_pillbox"),
                sections = listOf(
                    FileSection(
                        id = "sec_001",
                        heading = "Examen",
                        content = "Lord Arthur Blackwood, de cincuenta y ocho años, fue examinado en el estudio a las 12:40 a. m. Las pupilas estaban dilatadas. La boca estaba seca. El corazón se había acelerado antes de detenerse. Estos signos corresponden a la belladona. No corresponden a la digitalina. Las tabletas del pastillero abierto están intactas y no fueron la causa de la muerte.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_001",
                                text = "Estos signos corresponden a la belladona.",
                                description = "La dosis mortal fue de belladona, no del tónico cardíaco.",
                            ),
                        ),
                        references = listOf(
                            DocReference("item", "item_pillbox", "Pastillero de plata"),
                            DocReference("suspect", "suspect_reid", "Dr. Alistair Reid"),
                        ),
                    ),
                    FileSection(
                        id = "sec_002",
                        heading = "Escena",
                        content = "El cuerpo estaba desplomado sobre el secante. Una copa de cristal se encontraba al alcance de su mano derecha. La lámpara del estudio aún estaba caliente. Nadie había abierto las ventanas.",
                        image = ImageRef(SCENE, "El escritorio de Lord Arthur en el estudio, fotografiado antes de retirar la copa."),
                        references = listOf(
                            DocReference("item", "item_brandy", "Copa de brandy"),
                        ),
                    ),
                ),
            ),

            CaseFile(
                id = "file_002",
                title = "Certificado de laboratorio",
                documentNumber = "CC-1924-014-B",
                date = "16 de noviembre de 1924",
                thumbnail = "images/cases/case_001/items/item_brandy.png",
                shortDescription = "El residuo en la copa y las huellas encontradas en el borde.",
                relatedSuspectIds = listOf("suspect_reid", "suspect_morris"),
                relatedItemIds = listOf("item_brandy", "item_vial"),
                sections = listOf(
                    FileSection(
                        id = "sec_003",
                        heading = "Análisis químico",
                        content = "El residuo blanco en el interior de la copa es belladona concentrada. Corresponde a una tintura de boticario, no a un amargo de cocina. Aproximadamente dos dracmas fueron vertidas en el brandy después de que la copa ya había sido servida.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_002",
                                text = "El residuo blanco en el interior de la copa es belladona concentrada.",
                                description = "Alguien envenenó directamente el brandy.",
                            ),
                        ),
                        references = listOf(
                            DocReference("item", "item_brandy", "Copa"),
                            DocReference("item", "item_vial", "Frasco ámbar"),
                        ),
                    ),
                    FileSection(
                        id = "sec_004",
                        heading = "Huellas",
                        content = "Las huellas en el borde de la copa coinciden con las del Dr. Alistair Reid. Las huellas del mayordomo aparecen únicamente en el asa de la bandeja. No se encontraron huellas de Lady Eleanor ni de Julian Vance en la copa.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_005",
                                text = "Las huellas en el borde de la copa coinciden con las del Dr. Alistair Reid.",
                                description = "La última mano que tocó el borde de la copa fue la del doctor.",
                            ),
                        ),
                        references = listOf(
                            DocReference("suspect", "suspect_reid", "Dr. Reid"),
                            DocReference("suspect", "suspect_morris", "Morris Graves"),
                        ),
                    ),
                ),
            ),

            CaseFile(
                id = "file_003",
                title = "Declaración de Morris Graves",
                documentNumber = "CC-1924-014-C",
                date = "15 de noviembre de 1924",
                thumbnail = "images/cases/case_001/suspects/suspect_morris.png",
                shortDescription = "Quién sirvió el brandy y quién entró cuando él salía.",
                relatedSuspectIds = listOf("suspect_morris", "suspect_reid", "suspect_julian"),
                relatedItemIds = listOf("item_brandy"),
                sections = listOf(
                    FileSection(
                        id = "sec_005",
                        heading = "La bandeja",
                        content = "Serví el brandy de su señoría a las diez cuarenta y dejé la bandeja sobre el escritorio. Estaba vivo y bastante molesto por lo de la firma de medianoche. Salí del estudio a las diez cuarenta y dos. No regresé hasta que lo encontré.",
                    ),
                    FileSection(
                        id = "sec_006",
                        heading = "La puerta",
                        content = "El Dr. Reid entró mientras yo salía y dijo que solo tardaría un momento en tomarle el pulso a su señoría. Llevaba su maletín negro. El señor Julian estuvo encerrado en la sala de billar desde las diez. Yo mismo tuve que abrirle cuando se dio la alarma.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_003",
                                text = "El Dr. Reid entró mientras yo salía y dijo que solo tardaría un momento en tomarle el pulso a su señoría.",
                                description = "Reid entró al estudio después de que el brandy había sido servido.",
                            ),
                        ),
                        references = listOf(
                            DocReference("suspect", "suspect_reid", "Dr. Reid"),
                            DocReference("suspect", "suspect_julian", "Julian Vance"),
                            DocReference("file", "file_004", "Nota del abogado"),
                        ),
                    ),
                ),
            ),

            CaseFile(
                id = "file_004",
                title = "Memorando del abogado",
                documentNumber = "CC-1924-014-D",
                date = "14 de noviembre de 1924",
                thumbnail = "images/cases/case_001/items/item_telegram.png",
                shortDescription = "Lo que el codicilo de medianoche estaba a punto de terminar.",
                relatedSuspectIds = listOf("suspect_reid", "suspect_eleanor", "suspect_julian"),
                relatedItemIds = listOf("item_telegram"),
                sections = listOf(
                    FileSection(
                        id = "sec_007",
                        heading = "El codicilo",
                        content = "Lord Blackwood me indicó que acudiera a medianoche. El nuevo codicilo destina fondos a un hospital público. También pone fin a la asignación vitalicia del Dr. Reid y a los honorarios privados asociados. El patrimonio de Lady Eleanor ya está protegido y no se verá afectado. La herencia que Julian Vance esperaba recibir queda reducida a una cantidad simbólica.",
                        highlightableClues = listOf(
                            ClueDef(
                                id = "hl_004",
                                text = "El codicilo de medianoche pone fin a la asignación vitalicia del Dr. Reid y a los honorarios privados asociados.",
                                description = "Reid tenía una fortuna que perder antes del amanecer.",
                            ),
                        ),
                        references = listOf(
                            DocReference("suspect", "suspect_reid", "Dr. Reid"),
                            DocReference("suspect", "suspect_eleanor", "Lady Eleanor"),
                            DocReference("suspect", "suspect_julian", "Julian Vance"),
                        ),
                    ),
                ),
            ),
        ),

        suspects = listOf(
            Suspect(
                id = "suspect_eleanor",
                name = "Lady Eleanor Blackwood",
                alias = "La Viuda",
                portrait = "images/cases/case_001/suspects/suspect_eleanor.png",
                shortDescription = "Viuda de Lord Arthur. Conserva la mansión, el invernadero y un patrimonio que no depende del nuevo testamento. Se muestra fría durante el interrogatorio y es precisa con los horarios.",
                occupation = "Señora de la mansión",
                knownInformation = "Estuvo casada con Arthur durante veintidós años. Su propia fortuna está protegida legalmente. Lleva un registro nocturno en el invernadero.",
                alibi = "Afirma que estuvo en el invernadero cuidando el jazmín nocturno desde las diez hasta que el mayordomo dio la alarma. La tierra de sus zapatos coincide con la de la mesa de jardinería.",
                questions = listOf(
                    Question(
                        id = "q_e1",
                        question = "¿Dónde estuvo desde las diez hasta que se dio la alarma?",
                        answer = "En el invernadero, con el jazmín nocturno. El libro de registro está sobre la mesa de jardinería. No entré al estudio.",
                    ),
                    Question(
                        id = "q_e2",
                        question = "¿El nuevo testamento la perjudicaba?",
                        answer = "Arthur me lo dijo durante la cena. Mi patrimonio ya está asegurado. No pierdo nada si el hospital recibe el resto.",
                    ),
                    Question(
                        id = "q_e3",
                        question = "¿A quién vio en el pasillo?",
                        answer = "Al Dr. Reid, con su maletín negro, caminando hacia el estudio cuando el reloj del vestíbulo marcó las diez y cuarenta y cinco.",
                        revealsClue = "hl_003",
                    ),
                ),
            ),

            Suspect(
                id = "suspect_julian",
                name = "Julian Vance",
                alias = "El Sobrino",
                portrait = "images/cases/case_001/suspects/suspect_julian.png",
                shortDescription = "Sobrino de Arthur. Encantador, endeudado y esperando una herencia que el nuevo testamento prácticamente eliminaría. Jura que nunca salió de la sala de billar.",
                occupation = "Caballero desempleado",
                knownInformation = "Vive de una asignación. Los jugadores de la ciudad conocen bien su nombre. Le pidió a Arthur que no firmara.",
                alibi = "Afirma que la puerta de la sala de billar se atascó y quedó encerrado desde las diez hasta que Graves la abrió. El mayordomo confirma el problema con la cerradura.",
                questions = listOf(
                    Question(
                        id = "q_j1",
                        question = "Explique el telegrama roto.",
                        answer = "Debo dinero. Eso no me convierte en asesino. Estaba encerrado en la sala de billar, gritando para que Graves me sacara.",
                        revealsItem = "item_telegram",
                    ),
                    Question(
                        id = "q_j2",
                        question = "¿Discutió con su tío?",
                        answer = "Le pedí que no firmara. Se rio de mí. Fui a la sala de billar a las diez y permanecí allí.",
                    ),
                    Question(
                        id = "q_j3",
                        question = "¿Tocó el brandy?",
                        answer = "Yo bebo whisky. Nunca toco su brandy. Pregúntele a Graves quién manipuló la copa.",
                    ),
                ),
            ),

            Suspect(
                id = "suspect_morris",
                name = "Morris Graves",
                alias = "El Mayordomo",
                portrait = "images/cases/case_001/suspects/suspect_morris.png",
                shortDescription = "Mayordomo durante diecinueve años. Sirvió el brandy, apagó las lámparas y encontró a su amo desplomado sobre el escritorio.",
                occupation = "Mayordomo",
                knownInformation = "Sirvió el brandy de la noche. Sus huellas están en la bandeja, no en el borde de la copa. Dio la alarma a las 11:17.",
                alibi = "Estuvo en el salón del servicio después de las 10:42, puliendo plata con la criada, hasta que regresó al estudio y encontró el cuerpo.",
                questions = listOf(
                    Question(
                        id = "q_m1",
                        question = "Cuénteme exactamente qué hizo con el brandy.",
                        answer = "Lo serví a las diez cuarenta, dejé la bandeja sobre el escritorio y salí a las diez cuarenta y dos. Su señoría estaba vivo y de mal humor.",
                    ),
                    Question(
                        id = "q_m2",
                        question = "¿Quién pasó junto a usted en la puerta?",
                        answer = "El Dr. Reid entró mientras yo salía y dijo que solo tardaría un momento en tomarle el pulso a su señoría.",
                        revealsClue = "hl_003",
                    ),
                    Question(
                        id = "q_m3",
                        question = "¿Qué sabe del pastillero abierto?",
                        answer = "Estaba sobre el secante. Las tabletas seguían en sus compartimentos. No las toqué. Parecían sus pastillas habituales para el corazón.",
                        revealsItem = "item_pillbox",
                    ),
                ),
            ),

            Suspect(
                id = "suspect_reid",
                name = "Dr. Alistair Reid",
                alias = "El Médico",
                portrait = "images/cases/case_001/suspects/suspect_reid.png",
                shortDescription = "Médico de la familia. Controla el tónico cardíaco de Arthur, recibe una asignación vitalicia y lleva un maletín negro con olor a hierbas amargas.",
                occupation = "Médico de la familia",
                knownInformation = "Trató el corazón de Arthur durante una década. El codicilo de medianoche pondría fin a su asignación. Su maletín estaba en la casa.",
                alibi = "Afirma que estaba en la sala de armas escribiendo una receta y que no entró al estudio después de la cena.",
                questions = listOf(
                    Question(
                        id = "q_r1",
                        question = "¿Entró al estudio después de la cena?",
                        answer = "No. Estaba en la sala de armas escribiendo una receta para la cocinera. Quien diga lo contrario está equivocado.",
                    ),
                    Question(
                        id = "q_r2",
                        question = "¿Qué contiene el frasco ámbar?",
                        answer = "Belladona, diluida para preparar un linimento. No lo he abierto en una semana. Lo encontrará en mi maletín si insiste en registrar las pertenencias de un médico.",
                        revealsItem = "item_vial",
                    ),
                    Question(
                        id = "q_r3",
                        question = "¿Por qué iban a cancelar su asignación?",
                        answer = "Arthur malinterpretó una dosis. Estaba alterado. Pensaba explicárselo por la mañana. Nadie mata a un hombre por unos honorarios.",
                    ),
                ),
            ),
        ),

        items = listOf(
            Item(
                id = "item_brandy",
                name = "Copa de brandy de cristal",
                image = "images/cases/case_001/items/item_brandy.png",
                shortNotes = "Residuo blanco en la copa. Huellas de Reid en el borde.",
                description = "La copa nocturna de Lord Arthur, recogida del escritorio. El brandy está medio bebido. Una película blanquecina permanece adherida al interior.",
                findings = "El residuo es belladona concentrada. Las huellas del borde coinciden con las del Dr. Alistair Reid. Las huellas de Morris Graves aparecen únicamente en el asa de la bandeja.",
                relatedFileIds = listOf("file_001", "file_002", "file_003")
            ),
            Item(
                id = "item_pillbox",
                name = "Pastillero con monograma",
                image = "images/cases/case_001/items/item_pillbox.png",
                shortNotes = "Caja de plata marcada A.R. Las tabletas de digitalina están intactas.",
                description = "Un antiguo pastillero de plata encontrado abierto sobre el secante. La tapa lleva grabadas las iniciales A.R. Todos los compartimentos contienen sus tabletas.",
                findings = "Las tabletas contienen digitalina, están completas y no fueron envenenadas. El monograma coincide con el estuche de prescripciones del Dr. Alistair Reid. La forense descarta las pastillas como causa de muerte.",
                relatedFileIds = listOf("file_001"),
            ),
            Item(
                id = "item_telegram",
                name = "Telegrama de deuda roto",
                image = "images/cases/case_001/items/item_telegram.png",
                shortNotes = "Requerimiento de Western Union dirigido a Julian Vance.",
                description = "Un telegrama de Western Union, roto y posteriormente alisado. Exige el pago de una deuda de juego antes de finalizar el mes.",
                findings = "No contiene rastros de veneno ni huellas útiles. Demuestra la deuda de Julian, pero nada relacionado con la copa.",
                relatedFileIds = listOf("file_004"),
            ),
            Item(
                id = "item_vial",
                name = "Frasco de belladona",
                image = "images/cases/case_001/items/item_vial.png",
                shortNotes = "Frasco ámbar de boticario encontrado en el maletín de Reid. Faltan dos dracmas.",
                description = "Un pequeño frasco ámbar etiquetado como Tintura de Belladona, recuperado del maletín negro del Dr. Reid en la sala de armas.",
                findings = "Faltan aproximadamente dos dracmas. La cantidad coincide con la concentración encontrada en la copa. El tapón fue abierto recientemente.",
                relatedFileIds = listOf("file_002"),
            ),
        ),

        solution = CaseSolution(
            guiltySuspectId = "suspect_reid",
            requiredEvidenceIds = listOf("item_brandy", "item_vial", "hl_003"),
            minEvidenceCount = 2,
            explanation = "El Dr. Alistair Reid envenenó el brandy con belladona de su propio frasco después de que Graves abandonara el estudio. Sus huellas están en el borde de la copa. El pastillero de digitalina fue una distracción. Arthur estaba a pocas horas de cancelar la asignación de Reid, y Reid no podía permitir que aquel documento fuera firmado.",
            failureHints = mapOf(
                "suspect_eleanor" to "Los zapatos de Lady Eleanor tienen tierra húmeda del invernadero y su patrimonio ya estaba protegido. El dolor no es culpabilidad. Fíjate de nuevo en quién tocó la copa.",
                "suspect_julian" to "El sobrino tiene deudas, pero eso no lo convierte en asesino. Graves tuvo que abrir la sala de billar para dejarlo salir. Sigue el dinero de la asignación, no el telegrama.",
                "suspect_morris" to "Graves sirvió la bandeja y sus huellas solo aparecen allí. El borde de la copa cuenta otra historia. El mayordomo encontró el cuerpo; no preparó el veneno.",
                "suspect_reid" to "Reid es el hombre. Pero no puedo detenerlo por una corazonada. Necesito la copa, el frasco de belladona y el testimonio de Graves que lo sitúa entrando al estudio.",
            ),
        ),

        phoneMessages = listOf(
            PhoneMessage(
                id = "msg_001",
                caller = "Inspector Harlan Crowe",
                time = "11:22 p. m.",
                message = "Oficina, habla Crowe. Lord Blackwood está muerto en el estudio. Sellen la casa. Hay cuatro personas dentro: la viuda, el sobrino, el mayordomo y el Dr. Reid. Nadie sale.",
                unread = true,
            ),
            PhoneMessage(
                id = "msg_002",
                caller = "Forense Ada Quinn",
                time = "1:05 a. m.",
                message = "Pupilas muy dilatadas, piel enrojecida, garganta seca. Es belladona, no la digitalina de su pastillero. Busca una tintura concentrada. Las pastillas no lo mataron.",
                unread = true,
            ),
        ),
    ),
)
