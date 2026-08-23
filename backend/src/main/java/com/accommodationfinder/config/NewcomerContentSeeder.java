package com.accommodationfinder.config;

import com.accommodationfinder.model.ChecklistItem;
import com.accommodationfinder.model.GuideArticle;
import com.accommodationfinder.repository.ChecklistItemRepository;
import com.accommodationfinder.repository.GuideArticleRepository;
import com.accommodationfinder.service.AidParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Loads the guidance content and the official figures.
 *
 * <p>Runs on every start, but only writes what is missing, so an admin's edits are never
 * overwritten. Separate from {@link DataSeeder} because this content is real - it should be
 * there whether or not you want the demo listings, including in production.
 */
@Component
@Order(1)
public class NewcomerContentSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NewcomerContentSeeder.class);

    private final ChecklistItemRepository checklistRepository;
    private final GuideArticleRepository guideRepository;
    private final AidParameters aidParameters;

    public NewcomerContentSeeder(ChecklistItemRepository checklistRepository,
                                 GuideArticleRepository guideRepository,
                                 AidParameters aidParameters) {
        this.checklistRepository = checklistRepository;
        this.guideRepository = guideRepository;
        this.aidParameters = aidParameters;
    }

    @Override
    @Transactional
    public void run(String... args) {
        aidParameters.seedMissing();
        seedChecklist();
        seedGuides();
    }

    // ------------------------------------------------------------- checklist

    private void seedChecklist() {
        int order = 0;

        item("passport", "Passport or national ID card", "Identity", ++order, false, false, true,
                "Every landlord will ask for this, and it is the first page of your file. Have a "
                        + "clear scan as well as the original - you will send it by email far more "
                        + "often than you show it in person.",
                "If your passport is being renewed, a receipt from your consulate is usually "
                        + "accepted alongside another photo ID.",
                null);

        item("visa", "Visa or residence permit", "Identity", ++order, true, false, true,
                "If you are not from the EU, you need a valid long-stay visa (VLS-TS) or a "
                        + "residence permit. Landlords check the expiry date against the length of "
                        + "the lease, so a permit expiring in three months makes a twelve-month "
                        + "lease harder to get.",
                "If you have only just arrived and your permit is still being processed, show the "
                        + "visa in your passport plus the receipt from your appointment. That is "
                        + "normal and most landlords understand it.",
                "https://administration-etrangers-en-france.interieur.gouv.fr/");

        item("ofii", "Validate your visa with OFII", "Identity", ++order, true, false, true,
                "Within three months of arriving you must validate your long-stay visa online. "
                        + "It costs a tax stamp and takes about fifteen minutes. Skip it and your "
                        + "visa stops being valid, which affects everything else - bank, CAF, "
                        + "renewing your permit.",
                "Do it as soon as you have a French address, even a temporary one. You can update "
                        + "the address later.",
                "https://administration-etrangers-en-france.interieur.gouv.fr/");

        item("enrolment", "Enrolment letter from your school", "Studies", ++order, false, true, true,
                "The attestation de scolarite or certificat de scolarite proves you are a student. "
                        + "It unlocks the student rate on almost everything: housing aid, transport, "
                        + "the canteen, insurance.",
                "If term has not started, the admission letter your school sent you works for most "
                        + "purposes.",
                null);

        item("guarantor", "A guarantor, or a Visale certificate", "Money", ++order, false, false, true,
                "This is the one that stops most international students. French landlords want "
                        + "someone living in France who promises to pay if you cannot - usually a "
                        + "parent. If your family is abroad, you cannot provide that.",
                "Apply for Visale. It is the French state acting as your guarantor, it is free, and "
                        + "it exists precisely for people in your situation. Do it before you start "
                        + "viewing flats - having the certificate in hand makes you a far stronger "
                        + "applicant.",
                "https://www.visale.fr/");

        item("income", "Proof of income or scholarship", "Money", ++order, false, false, true,
                "Payslips, a scholarship award letter, or a bank statement showing you can cover "
                        + "the rent. Landlords typically look for income around three times the rent, "
                        + "which very few students meet - which is exactly why the guarantor matters "
                        + "more than this does.",
                "No income yet? A parent's bank statement plus your Visale certificate is a normal "
                        + "and accepted combination.",
                null);

        item("bank", "French bank account and a RIB", "Money", ++order, false, false, true,
                "A RIB is the slip with your account details on it. Rent is almost always paid by "
                        + "direct debit from a French account, and CAF will only pay housing aid into "
                        + "one.",
                "Opening an account usually needs proof of address, and getting an address usually "
                        + "needs a bank account. Online banks and student accounts at the big banks "
                        + "are used to this and will accept a temporary address or your school's "
                        + "attestation.",
                null);

        item("insurance", "Home insurance (assurance habitation)", "Legal", ++order, false, false, true,
                "This is not optional in France - it is a legal requirement for a tenant, and the "
                        + "landlord will ask for the certificate before handing over the keys. It "
                        + "costs a student around 5 to 10 euros a month.",
                "Your bank will usually sell you one in ten minutes when you open the account, and "
                        + "that is the easiest route.",
                "https://www.service-public.fr/particuliers/vosdroits/F2123");

        item("dossier", "Your whole file as one PDF", "Practical", ++order, false, false, false,
                "Good flats get thirty applications. The person who sends one tidy PDF with "
                        + "everything in it, five minutes after the viewing, gets the flat over "
                        + "someone who sends six blurry photos the next day.",
                "Name it clearly - Dossier_Firstname_Lastname.pdf - and keep it on your phone so "
                        + "you can send it from anywhere.",
                "https://www.dossierfacile.logement.gouv.fr/");

        item("caf", "Apply to CAF once you have moved in", "Money", ++order, false, false, true,
                "Housing aid is never sent automatically. You apply online after you move in, and "
                        + "CAF pays from the month after your application - it is not backdated. "
                        + "Applying a month late is a month of money you simply do not get.",
                "You need your lease, your RIB and your permit. Do it in your first week, even if "
                        + "some documents are still coming.",
                "https://www.caf.fr/");

        item("etat-des-lieux", "Inventory check (etat des lieux)", "Legal", ++order, false, false, true,
                "A written record of the flat's condition, signed by both of you on the day you "
                        + "move in. Photograph every mark, every appliance, the meter readings. "
                        + "Without it, any existing damage becomes your damage when you leave, and "
                        + "your deposit pays for it.",
                "If the landlord suggests skipping it 'to save time', insist. This is the single "
                        + "most common way deposits disappear.",
                "https://www.service-public.fr/particuliers/vosdroits/F1290");

        item("transport", "Student transport pass", "Practical", ++order, false, true, false,
                "In Ile-de-France the Imagine R pass gives students unlimited travel for a fraction "
                        + "of the normal price. Most regions have an equivalent. It pays for itself "
                        + "within two weeks.",
                "Apply as soon as you have your enrolment letter - processing takes a couple of weeks.",
                null);
    }

    private void item(String key, String title, String category, int order,
                      boolean nonEuOnly, boolean studentsOnly, boolean essential,
                      String explanation, String ifYouCannot, String link) {

        if (checklistRepository.findByKey(key).isPresent()) {
            return;
        }
        ChecklistItem item = new ChecklistItem();
        item.setKey(key);
        item.setTitle(title);
        item.setCategory(category);
        item.setSortOrder(order);
        item.setNonEuOnly(nonEuOnly);
        item.setStudentsOnly(studentsOnly);
        item.setEssential(essential);
        item.setExplanation(explanation);
        item.setIfYouCannot(ifYouCannot);
        item.setOfficialLink(link);
        checklistRepository.save(item);
    }

    // ---------------------------------------------------------------- guides

    private void seedGuides() {
        int order = 0;

        guide("housing-aid", "Money", "money", ++order,
                "Housing benefit: the money almost nobody tells you about",
                "The state pays part of your rent. You have to ask for it, and most international "
                        + "students find out too late.",
                """
                French housing benefit is called APL, and it is paid by an organisation called CAF.
                It is not charity and it is not means-tested against your country of origin. If you
                rent a place in France and you are here legally, you can claim it - students
                included, foreign students included.

                **How much?** For a student in a studio it is commonly somewhere between 100 and 250
                euros a month. On a 750 euro studio that is the difference between affordable and
                not. Use the estimate on each listing here to see the likely figure, then check it
                on the official simulator.

                **The three things people get wrong.**

                First, they do not apply. The money is never sent automatically. You fill in a form
                on caf.fr after you move in.

                Second, they apply late. CAF pays from the month after you apply and does not
                backdate. Apply in month one, not month four - those three months are simply gone.

                Third, they assume it will affect their visa. It does not. Housing aid is not one of
                the benefits that counts against a residence permit application.

                **What you need:** your lease, a French bank account (RIB), your residence permit and
                your enrolment letter.

                **One catch worth knowing.** If your parents claim you as a dependant for French tax
                purposes, claiming APL yourself can reduce their tax relief. For a family living
                abroad this almost never applies, but it is why French students sometimes hesitate
                and you should not copy their hesitation.
                """,
                "https://www.caf.fr/");

        guide("guarantor-visale", "Money", "guarantor", ++order,
                "No guarantor in France? Visale solves it, free",
                "The single most common reason international students are refused a flat - and the "
                        + "free government scheme that fixes it.",
                """
                Almost every French landlord asks for a *garant*: someone who lives in France, earns
                a French salary, and signs a promise to pay your rent if you do not. It is usually a
                parent.

                If your family lives in another country, you cannot provide one. This is not a small
                inconvenience - it is the reason applications get rejected before anyone reads them.

                **Visale is the state doing that job for you.** Action Logement, a public body,
                guarantees your rent. If you cannot pay, they pay the landlord, and you repay them
                afterwards. It costs you nothing. Landlords accept it because the guarantee comes
                from the state rather than from a stranger abroad.

                **Who can get it.** Anyone aged 18 to 30, whatever their nationality, with a valid
                residence permit or student visa. Students qualify with no income at all.

                **Rent limits.** As of January 2026, up to 1,000 euros a month including charges in
                Ile-de-France, and 680 euros elsewhere. Above that, Visale will not cover the flat -
                which is a useful filter when you are searching.

                **Do this before you start viewing.** Go to visale.fr, create an account and get your
                certificate - it takes about two working days. You do not need to have chosen a flat.
                Then when you view somewhere, you can say you already have a state guarantee, and you
                move from the bottom of the pile to the top of it.
                """,
                "https://www.visale.fr/");

        guide("reading-an-advert", "Practical", "reading", ++order,
                "How to read a French rental advert",
                "Charges comprises, meuble, caution, preavis - the words that decide what you "
                        + "actually pay.",
                """
                French adverts use a small vocabulary that changes the real cost quite a lot.

                **Loyer** - the rent itself.

                **Charges comprises (CC)** - bills included. **Hors charges (HC)** means they are
                not, and you should ask how much they run to. A 700 euro flat HC with 80 euros of
                charges costs more than a 750 euro flat CC.

                **Meuble / non meuble** - furnished or unfurnished. Furnished has a legal definition:
                a bed, a hob, an oven or microwave, a fridge, crockery, a table and chairs, storage
                and lighting. If it is advertised as furnished and half of that is missing, it is not
                furnished. Furnished leases are typically one year, or nine months for a student.

                **Caution / depot de garantie** - the deposit. Capped by law at one month's rent for
                an unfurnished flat and two for a furnished one. Anyone asking for more is breaking
                the law.

                **Preavis** - notice period when you leave. One month for a furnished flat, three for
                an unfurnished one, though that drops to one in most cities because of the tense
                housing market.

                **Etat des lieux** - the inventory. Do it properly. See the checklist.

                **Frais d'agence** - agency fees, capped by law and charged only by agencies. A
                private landlord cannot charge them at all.

                **Colocation** - a shared flat. Cheaper, and the housing-aid calculation is slightly
                different, which the estimate here already accounts for.

                **DPE** - the energy rating, A to G. Since 2025 the worst-rated homes cannot legally
                be rented out. A low rating also means a heating bill that will surprise you in
                January.
                """,
                null);

        guide("avoiding-scams", "Safety", "safety", ++order,
                "How rental scams work, and how not to lose your money",
                "The pattern almost never changes. Once you have seen it, it is obvious.",
                """
                Rental fraud targets people who cannot visit before they arrive, do not know what a
                normal price looks like, and are running out of time. That is a precise description
                of an international student in August.

                **The script.** A flat appears at a price clearly below everything comparable. The
                owner is warm and responds quickly. They are abroad - working, a missionary, a
                diplomat - so nobody can show you around. They will post the keys once you transfer
                the deposit. They may send convincing photos, a scan of an ID, even a contract. Once
                the money moves, they stop replying.

                **The rules that make you safe.**

                Never send money before you have seen the flat. In person, or on a live video call
                where the person walks around and shows you things you ask them to show you. A
                recorded video proves nothing.

                Never use Western Union, MoneyGram, gift cards, or cryptocurrency. No real French
                landlord asks for these. A transfer to a French IBAN, after signing a lease, is
                normal.

                A deposit is capped at one or two months' rent. Anyone asking for six months, or for
                a "reservation fee" before a viewing, is not a landlord.

                You should end up with a written lease and a signed inventory. If someone avoids
                paperwork, that is the whole tell.

                Be suspicious of urgency. "Three other people want it, send the deposit today" is
                pressure, and pressure is the tool.

                **On this site** we score every listing for these patterns and show you what we
                found. Treat a warning as a reason to slow down, not proof of guilt - and if
                something feels wrong, use the report button. It genuinely helps the next person.
                """,
                null);

        guide("your-rights", "Legal", "rights", ++order,
                "Your rights as a tenant, in plain terms",
                "French tenancy law protects you strongly. Knowing three or four rules stops most "
                        + "problems.",
                """
                **You cannot be evicted in winter.** From 1 November to 31 March, the *treve
                hivernale* blocks evictions. This is not a licence to stop paying rent, but it means
                a dispute in December is not an emergency.

                **The deposit must come back within a month** if the inventory shows no damage, or
                two months if it does. After that the landlord owes you interest. This is why the
                move-in inventory matters so much - it is your evidence.

                **Discrimination is illegal.** A landlord may not refuse you because of your origin,
                your name, your religion or your nationality. They may assess whether you can pay.
                If you believe you were refused for who you are rather than what you earn, the
                Defenseur des Droits investigates this and it costs nothing to contact them.

                **The landlord cannot enter without your permission.** Not to inspect, not to show
                the flat to buyers, unless you agree a time.

                **Repairs are split.** Big things - boiler, roof, structural - are the landlord's.
                Small maintenance is yours. A broken heating system in winter is emphatically theirs.

                **Rent rises are limited** and in tight markets like Paris there is a rent cap on top
                of that. If your rent jumps mid-lease, check before you accept it.

                **If something goes wrong**, the first step is a registered letter (*lettre
                recommandee avec accuse de reception*). It sounds formal and it is - French disputes
                run on written proof. ADIL offices give free legal advice to tenants in every
                department, and they are used to helping people whose French is not fluent.
                """,
                "https://www.anil.org/");

        guide("first-two-weeks", "Practical", "calendar", ++order,
                "Your first two weeks, in order",
                "What to do first when everything needs doing at once.",
                """
                Everything depends on something else, which is why arriving feels like a deadlock.
                Here is an order that actually works.

                **Days 1-3.** Get a French SIM card - you need a French number for almost every form.
                Collect your enrolment letter from your school. If you are on a long-stay visa, start
                the OFII validation online; it has a three-month deadline and you do not want it
                hanging over you.

                **Days 3-7.** Open a bank account. Take your passport, your enrolment letter and any
                proof of address you have, even temporary. Ask for the RIB and for home insurance at
                the same appointment - the bank sells it and it saves you a separate errand.

                **Week 1, in parallel.** Apply for Visale. It takes two working days and you can do
                it before you have found a flat. Getting the certificate early is the single most
                useful thing you can do for your search.

                **Week 2.** View flats. Bring your file as one PDF. Apply the same day - speed is
                what wins in this market, not perfection.

                **The day you sign.** Do the inventory carefully. Photograph everything, note the
                meter readings, do not let anyone rush you.

                **The week you move in.** Apply to CAF. Set up electricity in your name. Register
                your address with your school. Get your transport pass.

                Then, and this is not a formality: find one person locally who has been here longer
                than you. Every one of these steps has a shortcut that nobody writes down.
                """,
                null);

        guide("where-to-live", "Practical", "housing", ++order,
                "Studio, colocation or student residence?",
                "The three options, what each really costs, and who each suits.",
                """
                **A studio of your own.** Most expensive, most independent. Around 700 to 900 euros
                in inner Paris, 500 to 700 in the inner suburbs, less further out. Housing aid helps
                most here in absolute terms. Best if you value quiet and can afford the gap.

                **Colocation - a room in a shared flat.** Usually 400 to 650 euros around Paris.
                Cheaper, and you arrive with people who already know how things work, which is worth
                more in your first month than you would guess. The housing-aid calculation uses a
                slightly lower ceiling for shared flats, which the estimates here already handle.
                Check whether the lease is individual or joint - with a joint one, if a flatmate
                stops paying, the landlord can come to you for their share.

                **CROUS student residences.** The cheapest option, often 250 to 400 euros, and
                housing aid applies on top. Rooms are small and demand vastly exceeds supply.
                Applications open early in the year through the *dossier social etudiant* and
                scholarship holders get priority. Apply even if you think you will not get it -
                it costs nothing but a form.

                **A note on location.** Somewhere twenty minutes further out on a direct metro line
                is usually better than somewhere nominally closer that needs two changes. Look at the
                journey, not the distance - the neighbourhood panel on each listing here shows you
                what is actually nearby and how long it takes to reach your campus.
                """,
                null);

        log.info("Newcomer guidance content is in place");
    }

    private void guide(String slug, String category, String icon, int order,
                       String title, String summary, String body, String link) {
        if (guideRepository.findBySlug(slug).isPresent()) {
            return;
        }
        GuideArticle article = new GuideArticle();
        article.setSlug(slug);
        article.setTitle(title);
        article.setSummary(summary);
        article.setBody(body);
        article.setCategory(category);
        article.setIcon(icon);
        article.setSortOrder(order);
        article.setOfficialLink(link);
        article.setPublished(true);
        article.setLastVerified(LocalDateTime.now());
        guideRepository.save(article);
    }
}
