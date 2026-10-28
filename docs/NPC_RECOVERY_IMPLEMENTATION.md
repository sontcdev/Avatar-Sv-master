# Milestone 3.7: NPC recovery

No gameplay code was added: no target-owned original menu, lesson, dialog, action rules or economic catalog for these NPCs was established. Target identities and positions below come from `database/avatar_2x.sql`; `chat` is `[]`. Native -61 reaches `Session.doCommunicate` (int target ID; NPC IDs use the 2,000,000,000 offset) and `NpcHandler.handlerCommunicate`, which checks zone and cooldown. Native -59 reaches `GlobalHandler.handleOptionMenu` (int target, byte menu ID, byte selection). This establishes common transport only: actual per-NPC menu IDs, selections, permissions, dialog, state transitions and side effects remain unknown. There is no verified original selection handler for these 13 NPCs. A generic fallback dialog is not recovered gameplay.

| ID | Target DB name | Map | x,y | Classification | Evidence gap |
| --- | --- | --- | --- | --- | --- |
| 540 | bac.thay.phu.thuy | 58 | 252,221 | BLOCKED | Original dialog and callback unknown. |
| 315 | giam.thi | 18 | 274,101 | BLOCKED | School menu, actions and rules unknown. |
| 645 | co.giao.tieng.anh | 6 | 150,100 | BLOCKED | Lesson, menu and state unknown. |
| 646 | co.giao.tieng.viet | 6 | 250,50 | BLOCKED | Lesson, menu and state unknown. |
| 50 | tien.ca | 30 | 180,81 | BLOCKED | Interaction and any catalog/pricing unknown. |
| 335 | phi.hanh.gia | 14 | 40,49 | BLOCKED | Interaction and any catalog/pricing unknown. |
| 425 | phu.thuy.bong.toi | 56 | 244,197 | BLOCKED | Interaction and any catalog/pricing unknown. |
| 590 | pretty.witch | 57 | 200,201 | BLOCKED | Interaction and any catalog/pricing unknown. |
| 760 | pharaoh | 33 | 240,49 | BLOCKED | Interaction and any catalog/pricing unknown. |
| 465 | ve.so | 11 | 638,68 | BLOCKED | Original interaction, costs and reward rules unknown. |
| 525 | Chủ Hôn | 10 | 278,68 | BLOCKED | Original interaction and any consent/state transitions unknown. |
| 535 | zombie | 55 | 402,40 | BLOCKED | Original interaction and effects unknown. |
| 741 | an.xin | 3 | 168,157 | BLOCKED | Original interaction and effects unknown. |

No donor ID or display-only text was treated as target behavior. The bounded target code/history, DB and available client-resource inspection did not yield the missing original rules; recovery from the permitted artifacts remains unproven, not established to be impossible if new authoritative material appears. Existing -61/-59 protocol is not evidence of actual menu IDs or of any completed vertical slice.

## Proposed custom alternative — approval required, not implemented

Independently versioned `custom-english-quiz-v1`, disabled by default: NPC 645 offers “Bài kiểm tra tiếng Anh” and “Thoát”. The quiz asks three questions in order, each with one choice: “Cat là gì?” (Mèo, Chó, Cá; Mèo correct), “Dog là gì?” (Chó, Gà, Chim; Chó correct), “Hello nghĩa là gì?” (Xin chào, Tạm biệt, Cảm ơn; Xin chào correct). At the end show “Bạn trả lời đúng X/3 câu.” The player may exit at each question; reconnect or leaving the interaction clears the per-session attempt. There is no fee, reward, inventory change or persistent state. Reject stale/duplicate callbacks and revalidate NPC proximity and interaction state. Implement and test a client-compatible menu and native UI only after explicit approval. This is new custom gameplay, not recovered original compatibility.
